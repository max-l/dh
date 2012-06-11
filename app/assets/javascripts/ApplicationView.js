
FB_AUTH = null

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
    	rememberLogin: function() {
        	GlobalUtils.createCookie("FB_WAS_LOGGED_IN","true")
        },
        wasLoggedIn: function() {
    		var c = GlobalUtils.readCookie("FB_WAS_LOGGED_IN")
    		return c == "true"
        },
        initialize: function() {
        	this.displayNotLoggedInPanel()
        	initFacebook(this)
        	$.ajaxSetup({
        		beforeSend: function(jqXHR, settings) {
        		  if(FB_AUTH)
        		    jqXHR.setRequestHeader("FBAuth", JSON.stringify(FB_AUTH))
        	    }
        	})
        },
        ready: function() {
        	var zis = this
        	FB.Event.subscribe('auth.login', function(response) {
                zis.loggedInFacebook(response.authResponse)
                var bv = new DecisionWidgetList(token, popBallot);
                bv.model.fetch({success: function() {}});
                zis.$('#mainPanel').html(bv.render().el);                
        	})
        	FB.Event.subscribe('auth.logout', function(response) {
        		zis.loggedOutFacebook()
        	})

            FB.getLoginStatus(function(response) {
            	
            	if (response.status === 'connected') {
            		zis.loggedInFacebook(response.authResponse)
            	}

            	this.$('#topSubPanel').html('')
                var bv = new DecisionWidgetList(token, popBallot);
                bv.model.fetch({success: function() {}});
                zis.$('#mainPanel').html(bv.render().el);
            });
            
        },
        loggedInFacebook: function(authResponse) {
        	if(FB_AUTH) return 
        	
        	var zis = this
        	FB_AUTH = authResponse
        	zis.setFbName()
        },
        loggedOutFacebook: function() {
        	FB_AUTH = null
        	this.$('#signIn').text('Sign in')
        	this.displayNotLoggedInPanel()
        	if(this.logOutMenuItem) this.logOutMenuItem.remove()
        	if(this.myFacebookDecisions) this.myFacebookDecisions.remove()
        },
        displayNotLoggedInPanel: function() {
        	var topPanel = $(Templates.welcomPanelTemplate())
//        	topPanel.find('#createNewDecision').click(function(){
//        		new CreateDecisionWizard()
//        	})
        	this.$('#topSubPanel').html(topPanel)
        },
        render: function() {
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
  			  
  			  zis.myFacebookDecisions = $("<li><a>My Decisions on Facebook</a></li>")
  			  
  			  zis.myFacebookDecisions.click(function() {
  				
                  var bv = new DecisionWidgetList();
                  bv.model.fetch()
                  zis.$('#mainPanel').html(bv.render().el);
  			  })
  			  
  			  zis.$('#menuBar').append(zis.myFacebookDecisions)
  			  
  			  zis.$('#topSubPanel').empty()
  			})
        }
   });
	 
   return new V()
}

GenericDialog = function(options, okCallback) {
	
   if(! options.okCaption) options.okCaption = "Ok"

   if(! options.cancelCaption) options.cancelCaption = "Cancel"

    var V = Backbone.View.extend({
    	events: {
           "click #ok": function() {
    	      if(okCallback) okCallback(this)
           },
           "click #cancel" : function() {
             this.modal.hide();
             $(this.el).remove()
           }
        },
        initialize: function() {
        	$(this.el).html($(Templates.genericDialogTemplate(options)))
        	
        	this.$('.modal-body').append(options.body);
        	this.modal = $(this.el).modal({backdrop: 'static'}).data('modal')
        	$('body').append(this.render().el)
        }
   })
   
   return new V()
}

GlobalUtils = {

    createCookie: function (name,value,days) {
    	days = 1
    	if (days) {
    		var date = new Date();
    		date.setTime(date.getTime()+(days*24*60*60*1000));
    		var expires = "; expires="+date.toGMTString();
    	}
    	else var expires = "";
    	document.cookie = name+"="+value+expires+"; path=/";
    },
    readCookie: function(name) {
    	var nameEQ = name + "=";
    	var ca = document.cookie.split(';');
    	for(var i=0;i < ca.length;i++) {
    		var c = ca[i];
    		while (c.charAt(0)==' ') c = c.substring(1,c.length);
    		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    	}
    	return null;
    },
    eraseCookie: function (name) {
    	GlobalUtils.createCookie(name,"",-1);
    },
    validateEmail: function (email) { 
        // http://stackoverflow.com/a/46181/11236
          var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
          return re.test(email);
    }
}
