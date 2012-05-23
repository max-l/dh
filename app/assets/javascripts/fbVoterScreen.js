

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

function initializeApp(invitationInfoWhenUnAuthorized) {

    VoterAppView = Backbone.View.extend({
    	el: $('#mainPanel'),
        initialize: function() {
        	initFacebook(this)
        	this.render()
        },
        render: function() {

          var BallotList = Backbone.Collection.extend({
              model: Backbone.Model,
              url: "/fbballots"
          });

          var bl = new BallotList();
          var ballotList = $('<div></div>');
          $(this.el).html(ballotList);

          bv = createBallotListView(ballotList, Templates);
          bv.setModel(bl);
          bv.render()
          bl.fetch()
        },
        ready: function() {},
        loggedInFacebook: function(meResp, fbAuthResponse) {},
        loggedOutFacebook: function() {}
    });
    
    AuthorizeView = Backbone.View.extend({
    	el: $('#mainPanel'),
    	events: {
    	  "click #authorizeApp" : function() {
    	    FB.login()
          }
        }, 
        initialize: function() {
           initFacebook(this)
        },
        render: function() {
          $(this.el).html(Templates.invitationForUnauthorizedTemplate(this.model))
        },
        ready: function() {
            this.render()
        },
        loggedInFacebook: function(meResp, fbAuthResponse) {
            new VoterAppView()
        },
        loggedOutFacebook: function() {}
    });    

  if(! invitationInfoWhenUnAuthorized) {
    new VoterAppView()
  }
  else {
	new AuthorizeView({model: invitationInfoWhenUnAuthorized})
  }
}
