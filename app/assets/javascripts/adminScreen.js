
function initializeApp(decisionId) {


    DecisionHubAdminApp = Backbone.Model.extend({
        canVote: true,
        canAdmin: true,
        username: null,
        authenticator: null,
        initialize: function() {},
        currentDecision: function() {throw "No Decision Loaded"},
        setCurrentDecision: function(dId) {
        	var d = new Decision({id: dId});
        	this.currentDecision = function() {return d}
        }
    });

    AdminView = Backbone.View.extend({
    	el: $('#mainPanel'),
        events: {},
        decisionView: _.once(function() {
     	    return createDecisionView($('#adminTab'), Templates)
        }),
        participantView: _.once(function(){
        	return createParticipantView($('#participantsTab'), Templates, decisionId)
        }),
        initialize: function() {
        	this.model.on('change', this.render, this)
        	this.render();
        },
        setModel: function(decisionHubAdminApp) {
        	this.model = decisionHubAdminApp;
        	this.decisionView().setModel(decisionHubAdminApp.currentDecision());
        	this.participantView().setModel(decisionHubAdminApp.currentDecision().getFBParticipants());

        	decisionHubAdminApp.currentDecision().fetch()
        	decisionHubAdminApp.currentDecision().getFBParticipants().fetch()

        	var DVModel = Backbone.Model.extend({
        		url: '/decision/' + decisionId
        	});
        	var dv = new DVModel()
        	
        	var dvp = createDecisionViewPanel($('#decisionViewPanel'))
        	dvp.setModel(dv)
        	dv.fetch()
        },
        render: function() {
            $(this.el).html(Templates.decisionEditorTemplate())
            
        }
    });


   var decisionHubAdminApp = new DecisionHubAdminApp();
   decisionHubAdminApp.setCurrentDecision(decisionId);

   var adminView = new AdminView({model: decisionHubAdminApp});
   adminView.setModel(decisionHubAdminApp)

}

