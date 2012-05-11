

function createBallotView(decisionHubApp, ballotModel, rootElement) {
     var V = Backbone.View.extend({
    	el: rootElement,
    	model: ballotModel,
        events: {
            "click a[altId]" : "_scoreAlternative"
        },
        initialize: function() {
            this.model.bind('change', this.render, this);
        },
        _scoreAlternative: function(e) {
        	var did = decisionHubApp.decisionId;
        	var altId = e.currentTarget.attr('altId');
        	var score = e.currentTarget.attr('score');

            var req = $.get("/vote/" + altId + "/" + score);

            req.done(function(msg) {});

        	req.fail(function(jqXHR, textStatus) {
        	  //TODO e.preventDefault() ?
        	})
        },
        render: function() {
        	var ballot = _.extend({}, this.model.toJSON());
        	var el = $(this.el);
            var _template =  Handlebars.compile($('#voteTemplate').html());

            el.html(_template(ballot));
            return this;
        }
    });

    return new V()
 }
