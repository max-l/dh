
!function() {

    LoginView = Backbone.View.extend({
    	el: $('#mainPanel'),
    	events: {
    	  "click #createNewDecision" : function() {
    	     $('#loginDialog').modal('show')
          },
          "click #loginWithFacebook" : function() {
        	  initFacebook(this)
          },
          "click #cancel" : function() {
        	  $('#loginDialog').modal('hide')
          }
        },
        ready: function() {
        	FB.login()
        },
        loggedInFacebook: function(meResp, fbAuthResponse) {
        	var zis = this;
        	if(fbAuthResponse) {
                $.ajax({
                    type: 'POST',
                    url: "/loginWithFacebookToken",
                    data: JSON.stringify(fbAuthResponse),
                    success: function() {
                	  window.location = "/newDecision"
                    },
                    error: function() {
                    	var msg = $('<div class="alert alert-error">Failed to connect to Facebook or to authorize application.</div>')
                    	$(zis.el).find('#modalDialogMessage').html(msg)
                    },
                    contentType: "application/json; charset=utf-8",
                    dataType: 'json'
                })
            }
        },
        loggedOutFacebook: function() {},
        initialize: function() {
        	$('#loginDialog').modal('hide')
        },
        ok: function() {
            $(zis.el).find('#modalDialogMessage').html('')
        },
        render: function() {}
    });

    new LoginView()
}()
