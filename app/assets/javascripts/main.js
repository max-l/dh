
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
    
    
    MainView = Backbone.View.extend({
    	decisionHubApp: decisionHubApp,
        events: {
    	   'click a[href="#adminTab"]'        : '_adminTab',
           'click a[href="#voteTab"]'         : '_voteTab',
           'click a[href="#participantsTab"]' : '_participantsTab'
        },
        _adminTab: function() {
        	
        },
        _voteTab: function() {
        	
        },
        _participantsTab: function() {
        	
        }
    });

   createDecisionView(decisionHubApp, d, $("#adminTab"));
   
   Ballot = Backbone.Model.extend({
        defaults: function() {return {}}
   });
   
}