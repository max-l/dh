

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
        	
            $.ajax({
              type: 'POST',
              url: "/loginWithFacebookToken",
              data: JSON.stringify(authResponse),
              success: function() {
            	new ApplicationView(invitationInfoWhenUnAuthorized.decisionPublicGuid)
              },
              error: function() {},
              contentType: "application/json; charset=utf-8",
              dataType: 'json'
            })
        },
        loggedOutFacebook: function() {}
    });

    return new V()
}
