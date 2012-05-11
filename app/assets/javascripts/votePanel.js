

function createBallotView(decisionHubApp, ballotModel, rootElement) {
     var V = Backbone.View.extend({
    	el: rootElement,
    	model: ballotModel,
        events: {
            "click a[altId]" : "_scoreAlternative"
        },
        initialize: function() {
            this.model.bind('change', this.render, this);
            this.render()
        },
        _scoreAlternative: function(e) {
        	var did = decisionHubApp.decisionId;
        	var target = $(e.currentTarget);
        	var altId = target.attr('altId');
        	var score = target.attr('score');

            //var req = $.get("/vote/" + altId + "/" + score);
            //req.done(function(msg) {});
        	//req.fail(function(jqXHR, textStatus) {//TODO e.preventDefault() ?})
        },
        render: function() {
        	console.log(ballotModel);        	
        	var ballot = _.extend({}, this.model.toJSON());
        	console.log(ballot);
        	var el = $(this.el);
            var _template =  Handlebars.compile($('#voteTemplate').html());

            el.html(_template(ballot));
            return this;
        }
    });

    return new V()
 }
