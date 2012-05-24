


function createDecisionViewPanel(rootElement) {
     var V = Backbone.View.extend({
    	el: rootElement,
        events: {},
        initialize: function() {},
        setModel: function(decisionPublicDisplay) {
        	this.model = decisionPublicDisplay;
        	this.model.on('change', this.render, this)
        },
        render: function() {
        	
        	var rainbowNeg = new Rainbow();
        	var rainbowPos = new Rainbow();
        	
        	rainbowPos.setSpectrum('yellow', 'green');
        	rainbowNeg.setSpectrum('red',    'yellow');
        	
        	var decisionPublicDisplay = this.model.toJSON();
        	
        	_.each(decisionPublicDisplay.results, function(r) {
        		console.log(r)
        		if(r.percent > 50) {
        	      r.negColorCode = 0;
        	      r.negPercent = 0;
        	      r.posPercent = (r.percent -50) * 2;
        		  r.posColorCode = rainbowPos.colourAt(r.posPercent)
        		}
        		else {
        		  r.posPercent = 0;
        		  r.negPercent = (50 - r.percent) * 2;
        		  r.posColorCode = 0;
        		  r.negColorCode = rainbowNeg.colourAt(r.negPercent);
        		}
        	})
        	
            $(this.el).html(Templates.decisionPanelTemplate(decisionPublicDisplay));

            return this;
        }
    });

    return new V()
 }
