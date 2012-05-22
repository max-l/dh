
function augmentAndPostInfo(response, participantView) {

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
              url: "/dec/recordInvitationList",
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


function createParticipantView(rootElement, templates, decisionId) {
     var V = Backbone.View.extend({
    	el: rootElement,
        events: {
    	   'click #fbConnect' : function() {
    	     FB.login()
           },
           'click #inviteFromFB' : function() {
        	  var zis = this;
        	  var currentParticipants = 
        		  zis.model.map(function(p) {return p.get('facebookId')})
        		    .filter(function(fbId) {return fbId})

		      FB.ui({method: 'apprequests',
		          message: 'Zaza',
		          exclude_ids: currentParticipants,
		          data: decisionId
		        },
		        function requestCallback(response) {
		          if(response != null) {
		             response.decisionId = decisionId
		             augmentAndPostInfo(response, zis)
		           }
		        }
		      )
           }
        },
        loggedInFacebook: function(meResp, fbAuthResponse) {
        	var zis = this;
        	if(fbAuthResponse) {
                $.ajax({
                    type: 'POST',
                    url: "/loginWithFacebookToken",
                    data: JSON.stringify(fbAuthResponse),
                    success: function() {
                	  zis.loggedInFacebookAndDecisionHub(meResp, fbAuthResponse)
                    },
                    error: function() {
                    	$(zis.el).find('#fbConnect').text('Connect To FB')
                    },
                    contentType: "application/json; charset=utf-8",
                    dataType: 'json'
                })
            }
        },
        loggedInFacebookAndDecisionHub: function(meResp, fbAuthResponse) {
      	    $(this.el).find('#fbConnect').text('Connected To FB ' + meResp.name)
        },
        ready: function() {
        	this.render()
        },
        loggedOutFacebook: function() {
        	$(this.el).find('#fbConnect').text('Connect To Facebook')
        },
        initialize: function() {
        	initFacebook(this)
            $(this.el).html(templates.participantTabTemplate())
        },
        setModel: function(participantsList) {
        	this.model = participantsList
        },
        //render: function() {debugger;return this;},
    	setModel : function(list) {
        	this.model = list;
            this.model.on('add', this.addOne, this);
            this.model.on('all', this.render, this);
            this.model.on('reset', this.addAll, this);
        },
        addAll: function() {
        	var ul = this.$("ul");
        	ul.empty();
        	this.model.each(this.addOne, this);
        },
        addOne: function(pv) {
            var ul = this.$("ul");
            var FBParticipantView = Backbone.View.extend({
            	model: pv,
            	render: function() {
            	  console.log(pv.toJSON())
            	  $(this.el).html(templates.fbParticipantTemplate(pv.toJSON()))
            	  return this
                }
            });
            var view = new FBParticipantView();
            ul.append(view.render().el);
        }
    });

    return new V()
 }
