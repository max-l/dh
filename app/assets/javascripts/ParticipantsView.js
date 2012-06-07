
/**
 *    Invite participants from Facebook 
 */


ParticipantsView = function(decisionId, fbAppRerquestTitle) {
	
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
            $(this.el).html(Templates.participantTabTemplate())
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
            	  $(this.el).html(Templates.fbParticipantTemplate(pv.toJSON()))
            	  return this
                }
            });
            var view = new FBParticipantView();
            var li = this.make('li');
            $(li).append(view.render().el);
            this.$("ul").append(li)
        }
    });

    return new V()
}
