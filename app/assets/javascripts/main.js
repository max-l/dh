
function initializeApp(decisionId) {
	

    Decision = Backbone.Model.extend({
        defaults: function() {
            return {
                title: "",
                description: ""
            };
        },
        choiceList: function() {
          if(! this._choiceList) {
             var ChoiceList = Backbone.Collection.extend({
                 model: DynListElement,
                 url: "/dec/alternatives/" + this.id
             });
        	 this._choiceList = new ChoiceList({id: this.id})
          }
          return this._choiceList
        },
        urlRoot: "/dec",
        getBallot: function() {
        	var Ballot = Backbone.Model.extend({});
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
        decisionView: _.once(function() {
  	       return createDecisionView(decisionHubApp, $("#adminTab"))
        }),
        ballotView: _.once(function() {
    	    return createBallotView(decisionHubApp, $('#voteTab'))
        }),
        setCurrentDecision: function(dId) {

            var d = new Decision({id: dId});
            d.fetch();
            d.set('endTime', (new Date().getTime()));
            this.currentDecision = d;
        },
        resetCurrentDecision: function(did) {
        	this.setCurrentDecision(did);
        	this.decisionView().model = this.currentDecision;
        	this.decisionView().initialize();
        	this.decisionView().render();
        	this.ballotView().model = this.currentDecision.getBallot();
        	this.ballotView().initialize();
        	this.ballotView().render();
        	$('#adminTab').tab('show');
        },        
        templates: Templates
    });

    var decisionHubApp = new DecisionHubApp();

    decisionHubApp.setCurrentDecision(decisionId);


    MainView = Backbone.View.extend({
    	el: $('#mainPanel'),
    	decisionHubApp: decisionHubApp,
        events: {
    	   'click a[href=#adminTab]'        : '_adminTab',
           'click a[href=#voteTab]'         : '_voteTab',
           'click a[href=#participantsTab]' : '_participantsTab',
           'click a[id=zaza]' : 'a'
        },
        a: function() {
            decisionHubApp.resetCurrentDecision($('#aa').val())
        },
        initialize: function() {
            //TODO: pre initialize depending on permissions :
            this._adminTab()
        },
        _adminTab: function() {
            decisionHubApp.decisionView()
        },
        _voteTab: function() {
            decisionHubApp.ballotView()
        },
        _participantsTab: function() {
            
        }
    });

   new MainView()
}

