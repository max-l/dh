


ApplicationView = function(token, popBallot) {

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

          var bv = new DecisionWidgetList(token, popBallot);
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

GlobalUtils = {
    validateEmail: function (email) { 
        // http://stackoverflow.com/a/46181/11236
          var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
          return re.test(email);
    }
}