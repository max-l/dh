

FacebookAuthorizeDialog = function(invitationInfoWhenUnAuthorized) {

	var V = Backbone.View.extend({
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
          $(this.el).html(Templates.invitationForUnauthorizedTemplate(invitationInfoWhenUnAuthorized))
        },
        ready: function() {
            this.render()
        },
        loggedInFacebook: function(meResp, fbAuthResponse) {
            new ApplicationView()
        },
        loggedOutFacebook: function() {}
    });

    return new V()
}
