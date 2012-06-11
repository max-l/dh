

DecisionSettingsView = function(decisionId) {
	
	
    var V = Backbone.View.extend({
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
            "click #ok": "close",
            "click .close": "close",
            "click #terminateAutomaticYes" : function() {
            	this.model.set('automaticEnd', true)
            	this.model.save()
            },
            "click #terminateAutomaticNo" : function() {
            	this.model.set('automaticEnd', false)
            	this.model.save()
            }
        },
        initialize: function() {
        	var zis = this
    		this.model.fetch({
    			success: function() {
 			      $('body').append(zis.render().el)
    			  zis.modal = this.$('#adminDialog').modal({backdrop: false})
 		       }
 		    })
 		    /*
 		    this.model.on('change', function() {
 		    	if(zis.model.hasChanged('automaticEnd')) {
 		    		debugger
 		    		if(zis.model.get('automaticEnd'))
 		    		  zis.$('#terminateAutomaticYes').button('toggle')
 		    		else
 		    		  zis.$('#terminateAutomaticNo').button('toggle')
 		    	}
 		    })
 		    */
        },
        close: function() {
        	this.$('#adminDialog').modal('hide')
        	this.modal.remove()
        },
        render: function() {
        	//this.model.off('change');
        	var decision = this.model.toJSON();
        	$(this.el).html(Templates.decisionAdminTemplate(decision));

//        	this.endsOnField = 
//        	  this.$('#endTime').collapse({toggle: ! decision.endsOn}).data('collapse')

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
            
            zis._choicesListView.editingDisabled = decision.phase != "Draft";

            zis.$('#choiceList').html(zis._choicesListView.render().el)
            zis._choicesListView.model.fetch({success: function() {
                if(decision.phase != "Draft") {
                    zis.$('input').each(function(i, e) {
                    	$(e).prop('disabled', true)
                    })
                    zis.$('.icon-remove').each(function(i, e) {
                    	$(e).prop('disabled', true)
                    })
                }
            }})



            this.model.trigger('change')
            return this;
        }
    })

    return new V()
}