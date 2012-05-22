
function initializeApp(insideFbCanvas, isRegistered, appRequestInfo, decisionId, decisionTitle) {

	if(appRequestInfo) appRequestInfo.decisionTitle = decisionTitle;
      
    VoterApp = Backbone.Model.extend({
        canAdmin: true,
        username: null,
        authenticator: null,
        initialize: function() {},
        currentDecision: function() {throw "No Decision Loaded"},
        getBallot: _.once(function() {
            var Ballot = Backbone.Model.extend({});
            var B = Ballot.extend({
              decisionId: decisionId,
              url: '/ballot/' + decisionId
            });
            return new B()
        }),
        getBallotList: _.once(function() {
        	
            Ballot = Backbone.Model.extend({});
            
            FBParticipantsList = Backbone.Collection.extend({
                model: Ballot,
                url: "/dec/fbballots"
            });

            return new FBParticipantsList()
        }),
        setCurrentDecision: function(dId) {
        	var d = new Decision({id: decisionId});
        	this.currentDecision = function() {return d}
        }
    });

    VoterAppView = Backbone.View.extend({
    	el: $('#mainPanel'),
        events: {
           'click #authorizeApp' : function() {
              FB.login()
            }
        },
        initialize: function() {
        	if(insideFbCanvas) initFacebook(this)
        	else this.render();
        },
        setModel: function(voterApp) {
        	this.model = voterApp
        },
        ready: function() {
        	this.render();
        },
        render: function() {
        	if(! isRegistered)
                $(this.el).find("#infoPanel").html(Templates.invitationForUnauthorizedTemplate(appRequestInfo))
        	else {
        		if((! insideFbCanvas) && decisionId) {
        			this.ballotView()
        		}
        	}
        },
        ballotView: _.once(function() {
     	    return createBallotView($('#voteTab'), Templates)
        }),
        showBallotView: function(meResp, fbAuthResponse) {
        	this.ballotView().setModel(this.model.getBallot());
        	this.model.getBallot().fetch()
        },
        loggedInFacebook: function(meResp, fbAuthResponse) {
        	this.showBallotView()
        },
        loggedOutFacebook: function() {}
    });

    var app = new VoterAppView()
    app.setModel(new VoterApp())
}

