

function initializeApp(invitationInfoWhenUnAuthorized) {

    VoterAppView = Backbone.View.extend({
    	el: $('#mainPanel'),
        initialize: function() {
        	initFacebook(this)
        	this.render()
        },
        render: function() {

          var bv = new DecisionWidgetList();
          $(this.el).html(bv.render().el);
          bv.model.fetch()
        },
        ready: function() {},
        loggedInFacebook: function(meResp, fbAuthResponse) {},
        loggedOutFacebook: function() {}
    });
    
    AuthorizeView = Backbone.View.extend({
    	el: $('#mainPanel'),
    	events: {
    	  "click #authorizeApp" : function() {
    	    FB.login()
          }
        }, 
        initialize: function() {
           initFacebook(this)
        },
        render: function() {
          $(this.el).html(Templates.invitationForUnauthorizedTemplate(this.model))
        },
        ready: function() {
            this.render()
        },
        loggedInFacebook: function(meResp, fbAuthResponse) {
            new VoterAppView()
        },
        loggedOutFacebook: function() {}
    });    

  if(! invitationInfoWhenUnAuthorized) {
    new VoterAppView()
  }
  else {
	new AuthorizeView({model: invitationInfoWhenUnAuthorized})
  }
}
