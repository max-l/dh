
function initializeApp(decisionId) {
	
    ChoiceList = Backbone.Collection.extend({
        model: DynListElement,
        url: "/dec/alternative/" + decisionId
    });
	
    Ballot = Backbone.Model.extend({});

    Decision = Backbone.Model.extend({
        defaults: function() {
            return {
                title: "",
                description: ""
            };
        },
        choiceList: new ChoiceList(),
        urlRoot: "/dec",
        getBallot: function() {
        	var b0 = {
             		scores: [{
                		  alternativeId: 4, 
                		  title: 'Zaza Napoli', 
                		  currentScore: -1
                	    },
                		{
                  		  alternativeId: 5, 
                  		  title: 'Renato Baldi', 
                  		  currentScore: 1
                  	}]
                	};
        	var B = Ballot.extend({
        		url: '/ballot/' + this.id
            });
        	
        	var b = new B();

        	b.fetch();
        	
        	return b
        }
    });

    DecisionHubApp = Backbone.Model.extend({
        canVote: true,
        canAdmin: true,
        username: null,
        authenticator: null,
        fetchDecision: function(dId) {
            var d = new Decision({id: dId});
            d.fetch();
            d.set('endTime', (new Date().getTime()));
            this.currentDecision = d
        },
        templates: Templates
    });

    var decisionHubApp = new DecisionHubApp();

    decisionHubApp.fetchDecision(decisionId);


	var decisionView = _.once(function() {
	  createDecisionView(decisionHubApp, $("#adminTab"))
	});
	
	var ballotView = _.once(function() {
	  createBallotView(decisionHubApp, decisionHubApp.currentDecision.getBallot(), $('#voteTab'))
	});

    MainView = Backbone.View.extend({
    	el: $('#mainPanel'),
    	decisionHubApp: decisionHubApp,
        events: {
    	   'click a[href=#adminTab]'        : '_adminTab',
           'click a[href=#voteTab]'         : '_voteTab',
           'click a[href=#participantsTab]' : '_participantsTab'
        },
        initialize: function() {

        },
        _adminTab: function() {
            decisionView()
        },
        _voteTab: function() {
            ballotView()
        },
        _participantsTab: function() {
        	
        }
    });

   
   //TODO: pre initialize depending on permissions :
   decisionView();
   
   new MainView()
}

