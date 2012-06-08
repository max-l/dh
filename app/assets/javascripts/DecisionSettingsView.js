

DecisionSettingsView = function(decisionId) {
	
	
    var DecisionView = Backbone.View.extend({
    	model: new Decision({id: decisionId}),
        _choicesListView: new EditableListView({
        	collectionModel: new ChoiceList(decisionId),
            elementFieldName: "title"
        }),
        events: {
            "blur input[name=title]" : function(e) {
    	        var title = $(e.currentTarget).val();
    	        this.model.set('title', title)
    	        this.model.save()
            },
            "click #toggleEndsWhenComplete" : "toggleEndsWhenComplete",
            "click #toggleEndsAt" : "toggleEndsAt",
            "click a[id=phase]": function() {
            	var m = this.model
    			var p = m.get('phase')
    			if(p == "Draft") {
    				m.set('phase', 'VoteStarted')
    			}
    			else if(p == "VoteStarted") {
    				m.set('phase', 'Ended')
    			}
    			else if(p == "Ended") {
    				m.set('phase', 'VoteStarted')
    			}
    			this.model.save({success: function() {
    				debugger
    			}})
            }
        },
        setVotePossible: function(trueOrFalse) {
        	var b = 0
        	if(trueOrFalse) b = 1
        	else b = 0
        	var zis = this
        	$.get('/setVotePossible/' + decisionId + '/' + b, function() {
        		//zis.model.set('votingInProgress', trueOrFalse)
        		zis.model.reset()
        	})
        },
        displayPhase: function() {
			var p = this.model.get('phase')
			var btn = this.$('a[id=phase]')
			
			if(p == "Draft") {
				btn.text("Start Voting")
				btn.addClass('btn-success')
			}
			else if(p == "VoteStarted") {
				btn.text("End Vote and reveal results")
				btn.removeClass('btn-success')
				btn.addClass('btn-danger')
			}
			else if(p == "Ended") {
				btn.text("Extend Voting")
				btn.removeClass('btn-danger')
				btn.addClass('btn-success')
			}
			else throw Error("invalid phase")
        },
        initialize: function() {
        	var zis = this
        	this.model.on('change', function() {
        		if(zis.model.hasChanged('phase')) zis.displayPhase()
        	})
        },
        toggleEndsWhenComplete: function(e) {
        	this.model.set('endsOn', null);
          	this.model.save()
          	this.endsOnField.hide();
        },
        toggleEndsAt: function(e) {
        	if(! this.model.get('endsOn')) {
      	      this.model.set('endsOn', (new Date()).getTime());
        	}
      	    this.model.save()
      	    this.endsOnField.show();
        },
        render: function() {
        	//this.model.off('change');
        	var decision = this.model.toJSON();
        	$(this.el).html(Templates.decisionViewTemplate(decision));
        	
        	this.displayPhase()

        	if(decision.endsOn) this.$('#toggleEndsAt').button('toggle');
        	else this.$('#toggleEndsWhenComplete').button('toggle');

        	this.endsOnField = 
        	  this.$('#endTime').collapse({toggle: ! decision.endsOn}).data('collapse')

            var zis = this;
            var dateTimePickerOptions = {
                onChange: function(dt) {
    	           zis.model.set('endsOn', dt);
    	           zis.model.save()
            	},
            	minDate: new Date()
            };

            if(decision.endsOn)
              dateTimePickerOptions.defaultDatetime = new Date(decision.endsOn);

            this.$('div[data-provide=datetimepicker]').datetimepicker(dateTimePickerOptions);

            zis.$('#choiceList').html(zis._choicesListView.render().el)
            zis._choicesListView.model.fetch()

            var fbPartsView = new ParticipantsView(decisionId, "Invitation to vote on " + decision.title, this.model)
            $(this.el).append(fbPartsView.render().el)

            this.model.trigger('change')
            return this;
        }
    })

    return new DecisionView()
}