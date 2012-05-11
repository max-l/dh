
function initializeApp(decisionId) {
	
    ChoiceList = Backbone.Collection.extend({
        model: DynListElement,
        url: "/dec/alternative/" + decisionId
    });
	
    Decision = Backbone.Model.extend({
        defaults: function() {
            return {
                title: "",
                description: ""
            };
        },
        choiceList: new ChoiceList(),
        urlRoot: "/dec"
    });

    Ballot = Backbone.Model.extend({
        defaults: function() {return {}}
    });
    
    Ballot.fetch = function() {
    	return new Ballot({
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
      	})
    };

    DecisionHubApp = Backbone.Model.extend({
        canVote: true,
        canAdmin: true,
        username: null,
        authenticator: null,
        currentDecision: null
    });
    
    var decisionHubApp = new DecisionHubApp();
    
    var d = new Decision({id: decisionId});
    d.fetch();
    d.set('endTime', (new Date().getTime()));

	var decisionView = _.once(function() {
	  createDecisionView(decisionHubApp, d, $("#adminTab"))
	});
	
	var ballotView = _.once(function() {
	  createBallotView(decisionHubApp, Ballot.fetch(), $('#voteTab'))
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

