
function createBallotView(ballotModel, rootElement, templates, totalUpdater) {
     var V = Backbone.View.extend({
    	el: rootElement,
    	model: ballotModel,
        events: {
            "click a[altId]" : function(e) {

                var target = $(e.currentTarget);
                var altId = target.attr('altId');
                var score = target.attr('score');
                ballotModel.get('scores')[altId].currentScore = score

                totalUpdater(altId)
            }
        },
        render: function() {
        	var ballot = this.model.toJSON();
        	var el = $(this.el);

            el.html(templates.voteTemplate(ballot));

            _.each(ballot.scores, function(score) {
            	
            	if(score.currentScore === 0 || score.currentScore) {
            	  var e = el.find("a[score=" + score.currentScore + "][altId=" + score.alternativeId + "]")
            	  e.button('toggle');
            	}
            })
            

            return this;
        }
    });

    return new V()
 }
