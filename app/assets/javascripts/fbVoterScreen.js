

function createBallotListView(rootElement, templates) {
     var V = Backbone.View.extend({
    	el: rootElement,
        loggedInFacebook: function(meResp, fbAuthResponse) {},
        ready: function() {},
        loggedOutFacebook: function() {},
        initialize: function() {},
    	setModel : function(list) {
        	this.model = list;
            this.model.on('add', this.addOne, this);
            this.model.on('all', this.render, this);
            this.model.on('reset', this.addAll, this);
        },
        addAll: function() {
        	var ul = $(this.el);
        	this.model.each(this.addOne, this);
        },
        addOne: function(ballot) {
            var ul = $(this.el);
            var ballotDiv = $('<div></div>');
            ul.append(ballotDiv)
            var bv = createBallotView(ballotDiv, templates)
            bv.setModel(ballot)
            ballotDiv.append(bv.render())
        }
    });

    return new V()
 }

function initializeApp() {
      
    FBVoterApp = Backbone.Model.extend({
        canAdmin: true,
        username: null,
        authenticator: null,
        initialize: function() {},
        currentDecision: function() {throw "No Decision Loaded"},
        getBallotList: _.once(function() {

            var Ballot = Backbone.Model.extend({});

            BallotList = Backbone.Collection.extend({
                model: Ballot,
                url: "/fbballots"
            });

            return new BallotList()
        }),
        setCurrentDecision: function(dId) {
        	var d = new Decision({id: decisionId});
        	this.currentDecision = function() {return d}
        }
    });

    VoterAppView = Backbone.View.extend({
    	el: $('#mainPanel'),
        initialize: function() {
        	initFacebook(this)
        },
        render: function() {
          var bl = this.model.getBallotList();
          bv = createBallotListView($(this.el).find('#ballotList'), Templates);
          bv.setModel(bl);
          bv.render()
          bl.fetch()
        },
        setModel: function(voterApp) {
        	this.model = voterApp
        },
        ready: function() {},
        loggedInFacebook: function(meResp, fbAuthResponse) {},
        loggedOutFacebook: function() {}
    });

    var appView = new VoterAppView()
    appView.setModel(new FBVoterApp())
    appView.render()
}

!function() {
  initializeApp()
}()

