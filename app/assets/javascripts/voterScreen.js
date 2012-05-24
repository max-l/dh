
function initializeApp(decisionId) {

    VoterAppView = Backbone.View.extend({
    	el: $('#mainPanel'),
        initialize: function() {
        	//initFacebook(this)
        	this.render()
        },
        render: function() {
          
          var vp = createBallotView($('#ballotPanel'), Templates)
          return vp
        }
    });
}
