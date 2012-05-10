
    Decision = Backbone.Model.extend({
        defaults: function() {
            return {
                title: "",
                description: ""
            };
        },
        urlRoot: "/dec"
    });

    var _template =  Handlebars.compile($('#decisionViewTemplate').html());

    DecisionView = Backbone.View.extend({
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
            
            this.options.afterRender();

            return this;
        },
        change: function() {
          //this.model.save();
        }
    });
    
