

function createDecisionView(rootElement, templates) {
    var DecisionView = Backbone.View.extend({
    	el: rootElement,
        events: {
            "blur input[name=title]" : function(e) {
    	        var title = $(e.currentTarget).val();
    	        this.model.set('title', title)
    	        this.model.save()
    	        var btn = $(this.el).find('#inviteParticipants');
                if(title == "")
                  btn.attr('disabled', true)
                else 
                  btn.removeAttr('disabled')
              
            },
            "click #toggleEndsWhenComplete" : "toggleEndsWhenComplete",
            "click #toggleEndsAt" : "toggleEndsAt"
        },
        initialize: function() {},
        toggleTimeWidget: function(showOrHide) {
        	$(this.el).find('#endTime').collapse(showOrHide)
        },
        toggleEndsWhenComplete: function(e) {
        	this.model.set('endsOn', null);
          	this.toggleTimeWidget('hide');
          	this.model.save()
        },
        toggleEndsAt: function(e) {
        	if(! this.model.get('endsOn')) {
      	      this.model.set('endsOn', (new Date()).getTime());
        	}
      	    this.toggleTimeWidget('show');
      	    this.model.save()
        },
        setModel: function(decision) {
        	this.model = decision
        	this.model.on('change', this.render, this);
        },
        render: function() {
        	
        	this.model.off('change');
        	var decision = this.model.toJSON();
        	var el = $(this.el);

            el.html(templates.decisionViewTemplate(decision));

            if(decision.endsOn) {
              el.find('#toggleEndsAt').button('toggle');
              el.find('#endTime').attr('class', 'collapse')
              this.toggleTimeWidget('show')
            }
            else {
              this.toggleEndsWhenComplete()
              el.find('#toggleEndsWhenComplete').button('toggle');
              el.find('#endTime').attr('class', 'collapse in')
              this.toggleTimeWidget('hide')
            }

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

            var dp = el.find('div[data-provide=datetimepicker]').datetimepicker(dateTimePickerOptions);
            
            var clv = this._createChoicesListView($('#choiceList'));
        	this.model.choiceList().on('change', clv.render, clv);
        	clv.setModel(this.model.choiceList());
        	this.model.choiceList().fetch();

            return this;
        },
        _createChoicesListView: function(el) { 
            ChoiceView = DynListElementView.extend({
              compiledTemplate: templates.choiceTemplate
            });

            ChoicesListView = DynListView.extend({
              createElementView: function(e) {
                return new ChoiceView({model: e})
              },
              createNewElement: function(model, alternativeTitle) {
                return model.create({title: alternativeTitle})
              }
            });

            return new ChoicesListView({el: el})
        }
    })
    
    return new DecisionView()
}