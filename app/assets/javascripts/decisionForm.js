!function($) {

    var decision = Backbone.Model.extend({
        defaults: function() {
            return {
                id:1234,
                title: "zaza",
                description: ""
            };
        }
    });
    
    var _template =  Handlebars.compile($('#decisionViewTemplate').html());

    var DecisionView = Backbone.View.extend({

    	model: new decision(),
    	
    	el: $("#decisionForm"),
        events: {
            "blur input" : "change"
        },

        initialize: function() {
            this.model.bind('change', this.render, this);
            this.render()
        },

        render: function() {
            var dt = this.model; //.toJSON();
            debugger;
            $(this.el).html(_template(dt.toJSON()));
            return this;
        },

        change: function() {
	       //this.model.save();
        }
    });
    
    new DecisionView()

}(window.jQuery);
