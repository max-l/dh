
function initializeApp(decisionId) {

    VoterAppView = Backbone.View.extend({
    	el: $('#mainPanel'),
        initialize: function() {
        	//initFacebook(this)
        	this.render()
        },
        render: function() {
          
          var bv = createBallotView($('#ballotPanel'), Templates);
          
          var B = Backbone.Model.extend({
            url: "/ballot/" + decisionId
          });
          
          var ballot = new B();
          
          bv.setModel(ballot);
          ballot.fetch();
          
          return bv
        }
    });
    
    new VoterAppView()
}
