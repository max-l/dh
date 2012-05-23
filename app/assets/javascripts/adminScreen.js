
function initializeApp(decisionId) {

    Decision = Backbone.Model.extend({
        defaults: function() {
            return {title: ""}
        },
        choiceList: _.once(function() {
            var ChoiceList = Backbone.Collection.extend({
               model: DynListElement,
               url: "/dec/alternatives/" + this.id
            });
            return new ChoiceList({id: this.id})
        }),
        urlRoot: "/dec",
        getFBParticipants: _.once(function() {
            FBParticipant = Backbone.Model.extend({});

            FBParticipantsList = Backbone.Collection.extend({
                model: FBParticipant,
                url: "/dec/participants/" + this.id
            });

        	return new FBParticipantsList()
        })
    });

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
        events: {
    	   'click a[href=#adminTab]'        : '_adminTab',
           'click a[href=#voteTab]'         : '_voteTab',
           'click a[href=#participantsTab]'         : function() {
              this.participantView().model.fetch()
            }
        },
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
        },
        render: function() {
            $(this.el).html(Templates.decisionEditorTemplate())
            
        },
        _adminTab: function() {},
        _voteTab: function() {
        	this.model.currentDecision().getBallot().fetch()
        }
    });


   var decisionHubAdminApp = new DecisionHubAdminApp();
   decisionHubAdminApp.setCurrentDecision(decisionId);

   var adminView = new AdminView({model: decisionHubAdminApp});
   adminView.setModel(decisionHubAdminApp)
   decisionHubAdminApp.currentDecision().fetch()

}

