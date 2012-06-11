


DecisionWidget = function(decisionId, popBallot) {
	
  var DecisionView = function(decisionModel) {

        function rgbColor(rc, gc, bc) {
        	return {r:rc, g:gc, b:bc,
              asString: function() {
        	    return "rgb(" + this.r + "," + this.g + "," + this.b + ")"
              },
              inverse: function() {
            	return rgbColor(255 - this.r, 255 - this.g, 255 - this.b)
              }
            }
        }
        
        function interpolateColors(x1, x2, time) {
        	return rgbColor(
        		   Math.round(time * x1.r + (1-time) * x2.r),
        	       Math.round(time * x1.g + (1-time) * x2.g),
        	       Math.round(time * x1.b + (1-time) * x2.b))
        }
        
        function interpolateColorsR(x1, x2, min, max, n) {
        	var p = max - min
        	var time = 1 - (n / p);
        	return interpolateColors(x1, x2, time)
        }
	
     var V = Backbone.View.extend({
        model: decisionModel,
        events: {
    	 "click #voteNow" : "popupBallot",
         "click a[id=phaseBtn]": function() {
               	var m = this.model
               	var currentPhase = m.get('phase')
        			var nextPhase = null;
        			if(currentPhase == "Draft") {
        				nextPhase = 'VoteStarted'
        			}
        			else if(currentPhase == "VoteStarted") {
        				nextPhase = 'Ended'
        			}
        			else if(currentPhase == "Ended") {
        				nextPhase = 'VoteStarted'
        			} 
        			else throw Error('invalid phase')
        
        			$.get('/setDecisionPhase/' + decisionId + '/' + nextPhase, function(res) {
        				var r = JSON.parse(res)
        				m.set({'phase': nextPhase, results: r, viewerCanVote: ! r})
           		})
           },
           "click #registerToVote": function() {
        	   var b = $(Templates.sendEmailRegistrationTemplate())
        	   GenericDialog({title: "Register to Vote",body: b}, function(zisDialog) {

        		   var t = b.find('input').val()

                    $.ajax({
                        type: 'POST',
                        url: "/inviteByEmail/" + decisionId,
                        data: "[" + JSON.stringify(t) + "]",
                        success: function() {
                    	  zisDialog.modal.hide()
                    	  
                    	  GenericDialog({
                    		    title: "Registration was sent",
                    		    body: $(Templates.registrationSentTemplate({emailAddress: t})),
                    		    hideCancel: true
                    		  },
                    		  function(zisConfDialog) {zisConfDialog.modal.hide()}
                          )
                        },
                        error: function() {},
                        contentType: "application/json; charset=utf-8",
                        dataType: 'json'
                    })
        	   })
           }
        },
        popupBallot: function() {
    	    var b = new BallotModel({id: decisionId})
    	    var zis = this;
    	    b.fetch({success: function() {
        	    var bv = new BallotView(b, decisionModel);
        	    $(zis.el).append(bv.render().el)
        	    bv.show()
    	    }})
        },
        displayPhase: function(btn) {
			var p = this.model.get('phase')
			var phaseText = this.$('#phase')

			if(p == "Draft") {
				phaseText.text("Draft")
				btn.text("Start Voting")
				btn.addClass('btn-success')
			}
			else if(p == "VoteStarted") {
				phaseText.text("Vote in progress")
				btn.text("End Vote and reveal results")
				btn.removeClass('btn-success')
				btn.addClass('btn-danger')
			}
			else if(p == "Ended") {
				phaseText.text("Vote Ended")
				btn.text("Extend Voting (allow vote to continue)")
				btn.removeClass('btn-danger')
				btn.addClass('btn-success')
			}
			else throw Error("invalid phase")
        },
        initialize: function() {
        	this.model.on('change', this.render, this)
        	this.render()
        	if(popBallot && (! decisionModel.get('viewerHasVoted'))) this.popupBallot()
        },
        render: function() {

        	var decisionPublicDisplay = this.model.toJSON();

        	var z1 = rgbColor(255, 9,   5)
        	var z2 = rgbColor(255, 244, 94)
        	var z3 = rgbColor(45,  183, 14)

        	_.each(decisionPublicDisplay.results, function(r) {

        		if(r.percent <= 45)
        		  r.rgbColor = interpolateColorsR(z1, z2, 0, 45, r.percent).asString()
        		else if(r.percent <= 55)
        		  r.rgbColor =  z2.asString()
        		else
        		  r.rgbColor = interpolateColorsR(z2, z3, 0, 100, r.percent).asString()

        		if(r.percent < 40 || r.percent > 60)
        		  r.scoreColor = "white"
        		else
        		  r.scoreColor =  "black"

        		if(r.percent > 95)
        		  r.pos = 95
        		else
        		  r.pos = r.percent
        	})

            if(decisionPublicDisplay.viewerCanAdmin) {
            	if(decisionPublicDisplay.mode == "private-fb")
            		decisionPublicDisplay.adminPermissionsExplanation = "You created this decision, you can administer it, and vote because you have signed in with Facebook"
            	else
            		decisionPublicDisplay.adminPermissionsExplanation = "You are viewing this decision via it's administration link, you can modity it's settings and vote from this link."
            }
            
            if(decisionPublicDisplay.viewerCanVote && (! decisionPublicDisplay.viewerCanAdmin)) {
            	if(decisionPublicDisplay.mode == "private-fb")
            		decisionPublicDisplay.votePermissionsExplanation = "You are a participant in this decision, you can vote in it because you have signed in with Facebook"
            	else 
            		decisionPublicDisplay.votePermissionsExplanation = "You are viewing this decision with your personal voter's link, you can vote with this link."
            }

            $(this.el).html(Templates.decisionPanelTemplate(decisionPublicDisplay));
            

            var zis = this

			var phaseBtn = this.$('#phaseBtn')
			if(zis.model.hasChanged('phase')) zis.displayPhase(phaseBtn)
			
			if(zis.model.hasChanged('numberOfVoters'))
				zis.$('#numberOfVoters').text(zis.model.get('numberOfVoters'))

    		zis.displayPhase(phaseBtn)

            return this;
        }
    });

    return new V()
   }
	

   var BallotView = function(ballotModel, decisionPublicInfoModel) {
         var V = Backbone.View.extend({
        	model: ballotModel,
            events: {
                "click a[altId]" : function(e) {
                	var did = this.model.get('decisionId');
                	var target = $(e.currentTarget);
                	var altId = target.attr('altId');
                	var score = target.attr('score');
                    $.get("/vote/" + did + '/' + altId + '/' + score);
                },
                "click #submitVote" : function() {
        	       var zis = this;
        	       $.get('/submitVote/' + this.model.get('decisionId'), function(res) {
        	    	   decisionPublicInfoModel.set('numberOfVotesExercised', res)
        	    	   zis.hide()
        	       })
                },
                "click #close" : function() {
         	       this.hide()
                }
            },
            initialize: function() {
            	this.model.on('change', this.render, this)
            }, 
            render: function() {
    
            	var ballot = _.extend({}, this.model.toJSON());
            	var el = $(this.el);
    
                el.html(Templates.voteTemplate(ballot));
    
                _.each(ballot.scores, function(score) {
                	
                	if(score.currentScore === 0 || score.currentScore) {
                	  var e = el.find("a[score=" + score.currentScore + "][altId=" + score.alternativeId + "]")
                	  e.button('toggle');
                	}
                })
                
                this.modal = this.$('#votePanel').modal({backdrop: 'static'}).data('modal')
                
    
                return this;
            },
            show: function(){
            	this.modal.show()
            },
            hide: function(){
            	this.modal.hide()
            	$(this.el).remove()
            }
        });
    
        return new V()
      }

    var V = Backbone.View.extend({
    	events: {
          "click #admin" : function() {
	    	  new DecisionSettingsView(decisionId);
          },
          "click #inviteParticipants": function() {
        	  
              var fbPartsView = new ParticipantsView(decisionId, "Invitation to vote on " + this.decisionPublicInfo.get('title'), this.decisionPublicInfo)
              $('body').append(fbPartsView.render().el)
          }
        },
    	initialize: function() {
        	
    	}, 	
    	render: function() {
    		var zis = this 
    		var d = new DecisionPublicInfo({id: decisionId});
    		this.decisionPublicInfo = d

    		d.fetch({
    			success: function() {
    			   var p = DecisionView(d);
    			   $(zis.el).html(p.el);
    		    }
    		})

    		return this
    	}
    });
    
    return new V()	
}


DecisionWidgetList = function(token, popBallot) {

    var V = Backbone.View.extend({
        initialize: function() {
    	    if(token) {
    	        this.addOne(new Backbone.Model({decisionId: token}))
    	        //add a dummy model : 
    	        this.model = {fetch: function() {}}
    	    }
    	    else {
    	      this.model = new MyDecisionIds();
              this.model.on('add', this.addOne, this);
              this.model.on('reset', this.addAll, this);
    	    }
        },
        addAll: function() {
        	var ul = $(this.el);
        	this.model.each(this.addOne, this);
        },
        addOne: function(decisionIdModel) {
            var ul = $(this.el);
            var div = $('<div></div>')
            ul.append(div)
            var dv = new DecisionWidget(decisionIdModel.get('decisionId'), popBallot)
            div.append(dv.render().el)
        }
    });

   return new V()
}
