


ApplicationView = function(token, popBallot) {

    var V = Backbone.View.extend({
    	el: $('body'),
    	events: {
           "click #createNewDecision": function() {
	         new CreateDecisionWizard()
           },
           "click #signInWithFacebook": function() {
        	   FB.login()
           }
        },
        initialize: function() {
        	this.render()
        	initFacebook(this, true)
        },
        ready: function() {
            var zis = this
            
            FB.getLoginStatus(function(response) {
            	if (response.status === 'connected') {
            		//Connected to FB and App Authorized
            		//debugger
            		//GlobalUtils.logInClearVoteWithFbToken(response.authResponse, function() {zis.setFbName()})
            	}
            	else {
            		zis.displayNotLoggedInPanel()
            		//GlobalUtils.logOutClearVote()
            	}
            })
            
        },
        loggedInFacebook: function(meResp, authResponse) {
        	var zis = this
        	GlobalUtils.logInClearVoteWithFbToken(authResponse, function() {
        		zis.setFbName()
        	})
        },
        loggedOutFacebook: function() {
        	debugger
        	this.$('#signIn').text('Sign in')
        	this.displayNotLoggedInPanel()
        	this.logOutMenuItem.remove()
        },
        displayNotLoggedInPanel: function() {
        	var topPanel = $(Templates.welcomPanelTemplate())
        	topPanel.find('#createNewDecision').click(function(){
        		new CreateDecisionWizard()
        	})
        	this.$('#topSubPanel').html(topPanel)
        },
        render: function() {

          var bv = new DecisionWidgetList(token, popBallot);
          this.$('#mainPanel').html(bv.render().el);
          bv.model.fetch();
          return this
        },
        setFbName: function() {
        	var zis = this
			FB.api('/me', function(meResp) {
  			  zis.$('#signIn').text('logged in [' + meResp.name + ']')
  			  zis.logOutMenuItem = $("<li><a>Sign Out</a></li>")
  			  
  			  zis.logOutMenuItem.click(function() {
  				  FB.logout(function(){window.location.href = '/logout'})
  			  })
  			  
  			  zis.$('#menuBar').append(zis.logOutMenuItem)
  			  zis.$('#topSubPanel').html('')
  			  zis.render();
  			})
        }
   });
	 
   return new V()
}

GlobalUtils = {

    logInClearVoteWithFbToken: function(fbAuth, callback) {
        $.ajax({
            type: 'POST',
            url: "/loginWithFacebookToken",
            data: JSON.stringify(fbAuth),
            success: callback,
            error: function() {},
            contentType: "application/json; charset=utf-8",
            dataType: 'json'
          })
    },

    logOutClearVote: function(callback) {
        $.ajax({
            type: 'GET',
            url: "/logoutRest",
            success: callback
          })
    },

    validateEmail: function (email) { 
        // http://stackoverflow.com/a/46181/11236
          var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
          return re.test(email);
    }
}

