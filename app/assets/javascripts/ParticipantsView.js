
/**
 *    Invite participants from Facebook 
 */


ParticipantsView = function(decisionId, fbAppRerquestTitle, decisionModel) {
	
	if(! decisionId) throw new Error('decisionId is null.');
		
	
    var augmentAndPostInfo = function(response, participantView) {
    
        var fbIds = JSON.stringify(response.to).replace('[','(').replace(']',')')
    
        FB.api({
              method: 'fql.query',
              query: 'SELECT uid,name FROM user WHERE uid in ' + fbIds
           },
           function(queryResponse) {
                response.to = queryResponse
                var msgToPost = JSON.stringify(response)
                $.ajax({
                  type: 'POST',
                  url: "/dec/recordInvitationList/" + decisionId,
                  data: msgToPost,
                  success: function() {
                	//TODO: optimize...
                	participantView.model.fetch()
                  },
                  error: function() {
                  	//TODO: DELETE app Requests, and handle AppRequest Not Found
                  },
                  contentType: "application/json; charset=utf-8",
                  dataType: 'json'
                })
           }
        )
    }

    var modelz = Backbone.Collection.extend({
        model: Backbone.Model,
        url: "/dec/participants/" + decisionId
    });

     var V = Backbone.View.extend({
    	model: new modelz(),
        events: {
           'click #inviteFromFB' : function() {
        	   this.validateFbStatusAndPopInviteDialog()
           },
           'click #sendEmailInvitations': function() {
        	   var zis = this
        	   if(decisionModel.get('canInviteByEmail'))
        	     this.sendEmailInvitations()
        	   else if(decisionModel.get('mode') == "private-fb") {
            	   FB.login(function(response) {
            		   zis.sendEmailInvitations()
            	   }, {scope: 'email'})
        	   }
           }
        },
        sendEmailInvitations: function() {
        	var zis = this
        	var emailList = this.$('textarea[name=emailInvitations]').val().split(',')
        	var invalidemails = _.filter(emailList, function(e) {return ! validateEmail(e)})
        	if(invalidemails.length > 0)
        		alert("there are invalid emails: " + 
        			_.reduce(invalidemails, function(e1,e2){return e1 + ' ' + e2}))
        	else {
                $.ajax({
                    type: 'POST',
                    url: "/inviteByEmail/" + decisionId,
                    data: JSON.stringify(emailList),
                    success: function() {
                  	  zis.model.fetch()
                    },
                    error: function() {},
                    contentType: "application/json; charset=utf-8",
                    dataType: 'json'
                  })
        	}
        },
        loggedInFacebook: function(meResp, fbAuthResponse) {
        	var zis = this;
        	if(fbAuthResponse) {
        		//loginClearVote(meResp, fbAuthResponse)
            }
        },
        validateFbStatusAndPopInviteDialog: function() {
        	var zis = this
        	FB.getLoginStatus(function(response) {
        		if (response.status === 'connected') {
        			//Connected to FB and App Authorized
        			zis.popFbInviteDialog(response.authResponse)
        		}
        		else {
        	        FB.Event.subscribe('auth.statusChange', function(response) {
                        if(response.authResponse) {
                            FB.api('/me', function(meResp) {
                            	zis.popFbInviteDialog(response.authResponse)
                            })
                        }
                        //else {alert('')}
                    })
                    FB.login()
                }
            })
        },
        popFbInviteDialog: function(authResponse) {
        	var zis = this
            var currentParticipants = 
    		  zis.model.map(function(p) {return p.get('facebookId')})
    	        .filter(function(fbId) {return fbId})
    
            FB.ui({method: 'apprequests',
                message: fbAppRerquestTitle,
                exclude_ids: currentParticipants,
                data: decisionId
              },
              function requestCallback(response) {
                if(response != null) {
                   response.decisionId = decisionId
                   response.fbAuthResponse = authResponse
                   augmentAndPostInfo(response, zis)
                }
              }
            )
        },
        loggedInFacebookAndDecisionHub: function(meResp, fbAuthResponse) {
      	    //this.$('#fbConnect').text('Connected To FB ' + meResp.name)
        },
        ready: function() {
        	
        },
        loggedOutFacebook: function() {
        	//this.$('#fbConnect').text('Connect To Facebook')
        },
        initialize: function() {
        	initFacebook(this)
            this.model.on('add', this.addOne, this);
            this.model.on('reset', this.addAll, this);
            $(this.el).html(Templates.participantTabTemplate({canInviteByEmail: decisionModel.get('canInviteByEmail')}))
            this.render()
            this.model.fetch()
        },
        addAll: function() {
        	var ul = this.$("ul");
        	ul.empty();
        	this.model.each(this.addOne, this);
        },
        addOne: function(pv) {
            var FBParticipantView = Backbone.View.extend({
            	model: pv,
            	render: function() {
            	  $(this.el).html(Templates.participantTemplate(pv.toJSON()))
            	  return this
                }
            });
            var view = new FBParticipantView();
            var li = this.make('li');
            $(li).append(view.render().el);
            this.$("ul").append(li)
        }
    });

    var validateEmail = function (email) { 
      // http://stackoverflow.com/a/46181/11236
        var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(email);
    }
    
    return new V()
}
