

function createDecisionView(decisionHubApp, decisionModel, rootElement, afterRender) {

    var DecisionView = Backbone.View.extend({
    	model: decisionModel,
    	el: rootElement,
        events: {
            "blur input" : "change",
            "click #toggleEndsWhenComplete" : "toggleEndsWhenComplete",
            "click #toggleEndsAt" : "toggleEndsAt"
        },
        initialize: function() {
            this.model.bind('change', this.render, this);
        },
        toggleTimeWidget: function(showOrHide) {
        	$(this.el).find('#endTime').collapse(showOrHide)
        },
        toggleEndsWhenComplete: function(e) {
        	this.model.get('endsOn', null);
          	this.toggleTimeWidget('hide')
        },
        toggleEndsAt: function(e) {
      	    this.model.get('endsOn', (new Date()).getTime());
      	    this.toggleTimeWidget('show')
        },
        render: function() {
        	var decision = _.extend({}, this.model.toJSON());
        	var el = $(this.el);
        	var _template =  Handlebars.compile($('#decisionViewTemplate').html());

            el.html(_template(decision));
            $('div[data-provide=datetimepicker]').datetimepicker();

            if(decision.endsOn) {
              el.find('#toggleEndsAt').button('toggle');
              el.find('#endTime').attr('class', 'collapse')
              this.toggleTimeWidget('show')
            }
            else {
              el.find('#toggleEndsWhenComplete').button('toggle');
              el.find('#endTime').attr('class', 'collapse in')
              this.toggleTimeWidget('hide')
            }

	        this._createChoicesListView(decisionModel.choiceList);

            return this;
        },
        _createChoicesListView: function(choiceList) { 
            ChoiceView = DynListElementView.extend({
              compiledTemplate: Handlebars.compile(
                  '<input type="text" value="{{title}}"/><i class="icon-remove"></i>')
            });

            ChoicesListView = DynListView.extend({
              el: $('#choiceList'),
              model: choiceList,
              createElementView: function(e) {
                return new ChoiceView({model: e})
              },
              createNewElement: function(model, alternativeTitle) {
                return model.create({title: alternativeTitle})
              }
            });
            new ChoicesListView()
        },
        change: function() {
          //this.model.save();
        }
    })
    
    return new DecisionView()
}