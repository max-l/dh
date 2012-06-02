


ApplicationView = function(token) {

    var V = Backbone.View.extend({
    	el: $('body'),
    	events: {
           "click #createNewDecision": function() {
	         new CreateDecisionWizard()
           }
        },
        initialize: function() {
        	//initFacebook(this)
        	this.render()
        },
        render: function() {

          var bv = new DecisionWidgetList(token);
          this.$('#mainPanel').html(bv.render().el);
          bv.model.fetch();

          return this
        },
        ready: function() {},
        loggedInFacebook: function(meResp, fbAuthResponse) {},
        loggedOutFacebook: function() {}
   });
	 
   return new V()
}