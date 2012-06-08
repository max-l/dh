

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
            "click #toggleEndsAt" : "toggleEndsAt"
        },
        initialize: function() {
        	//this.model.on('change', this.render, this);
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

            return this;
        }
    })

    return new DecisionView()
}