
function createBallotView(decisionHubApp, rootElement) {
     var V = Backbone.View.extend({
    	el: rootElement,
    	model: decisionHubApp.currentDecision.getBallot(),
        events: {
            "click a[altId]" : "_scoreAlternative"
        },
        initialize: function() {
            this.model.on('change', this.render, this);
        },
        _scoreAlternative: function(e) {
        	var did = decisionHubApp.currentDecision.id;
        	var target = $(e.currentTarget);
        	var altId = target.attr('altId');
        	var score = target.attr('score');

            var req = $.get("/vote/" + did + '/' + altId + '/' + score);
            req.done(function(msg) {
            });
        	req.fail(function(jqXHR, textStatus) {
        		//TODO e.preventDefault() ?
        	})
        },
        render: function() {

        	
        	var ballot = _.extend({}, this.model.toJSON());
        	var el = $(this.el);

            el.html(decisionHubApp.templates.voteTemplate(ballot));

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
