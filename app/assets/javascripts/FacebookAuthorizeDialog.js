

FacebookAuthorizeDialog = function(invitationInfoWhenUnAuthorized) {

	var V = Backbone.View.extend({
    	el: $('#mainPanel'),
    	events: {
    	  "click #authorizeApp" : function() {
    	    FB.login()
          }
        }, 
        initialize: function() {
           initFacebook(this, true)
        },
        render: function() {
          $(this.el).html(Templates.invitationForUnauthorizedTemplate(invitationInfoWhenUnAuthorized))
        },
        ready: function() {
            this.render()
        },
        loggedInFacebook: function(meResp, fbAuthResponse) {
        	debugger
        	var zis = this
            $.ajax({
              type: 'POST',
              url: "/loginWithFacebookToken",
              data: JSON.stringify(fbAuthResponse),
              success: function() {
            	new ApplicationView(invitationInfoWhenUnAuthorized.decisionPublicGuid)
            	$(zis.el).remove()
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
