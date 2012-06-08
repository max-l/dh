


DecisionWidget = function(decisionId) {

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
        events: {},
        initialize: function() {
        	this.model.on('change', this.render, this)
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

            $(this.el).html(Templates.decisionPanelTemplate(decisionPublicDisplay));

            return this;
        }
    });

    return new V()
   }
	

   var BallotView = function(ballotModel) {
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
        	       $.get('/submitVote/' + this.model.get('decisionId'), function() {
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
    	  "click #voteNow" : "popupBallot" 
        },
        popupBallot:function() {
    	    var b = new BallotModel({id: decisionId})
    	    var zis = this;
    	    b.fetch({success: function() {
        	    var bv = new BallotView(b);
        	    $(zis.el).append(bv.render().el)
        	    bv.show()
    	    }})
        },
    	initialize: function() {
        	
    	},
    	render: function() {
    		var el = $(this.el);
    		var e = $('<div class="decisionWidget"></div>')
    		el.append(e)
    		
    		var d = new DecisionPublicInfo({id: decisionId});

    		d.fetch({
    			success: function() {
    			
    			   var p = DecisionView(d);
    			   
    			   if(d.get('viewerCanAdmin')) {
    				    e.html($(Templates.decisionWidgetTemplate({decisionId: decisionId})));
        			    var dpvTab = this.$('#decisionPublicView'+decisionId);
        			    dpvTab.html(p.render().el)
        			    var settingsTab = this.$('#settings'+ decisionId)
    				    
    		    		var dsv = new DecisionSettingsView(decisionId);
    		    		dsv.model.fetch({
    		    			success: function() {
    		 			      settingsTab.html(dsv.render().el)
    		 		       }
    		 		    })
    			   }
    			   else {
    				   e.html(p.render().el);
    			   }
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
            var dv = new DecisionWidget(decisionIdModel.get('decisionId'))
            div.append(dv.render().el)

            if(popBallot) dv.popupBallot()
        }
    });
    
   return new V()
}
