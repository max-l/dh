
function interpolateColors(x1, x2, time) {
	
	return {
		r: Math.round(time * x1.r + (1-time) * x2.r),
	    g: Math.round(time * x1.g + (1-time) * x2.g),
	    b: Math.round(time * x1.b + (1-time) * x2.b)
	}	
}

function interpolateColorsR(x1, x2, min, max, n) {
	var p = max - min
	var time = n / p;
	
	return interpolateColors(x1, x2, time)
}

function interpolateColorsRString(x1, x2, min, max, n) {
	var c = interpolateColorsR(x2, x1, min, max, n)
	
	return "rgb(" + c.r + "," + c.g + "," + c.b + ")"
}


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

        	var decisionPublicDisplay = this.model.toJSON();

        	var z1 = {r: 255, g:9,   b:5}
        	var z2 = {r: 255, g:244, b:94}
        	var z3 = {r: 45,  g:183, b:14}

        	_.each(decisionPublicDisplay.results, function(r) {

        		if(r.percent <= 45)
        		  r.rgbColor = interpolateColorsRString(z1, z2, 0, 45, r.percent)
        		else if(r.percent <= 55)
        		  r.rgbColor =  "rgb(255,244,94)" //interpolateColorsRString(z2, z2, 0, 55, r.percent)
        		else
        		  r.rgbColor = interpolateColorsRString(z2, z3, 0, 100, r.percent)

        		if(r.percent > 95)
        		  r.pos = 95
        		else
        		  r.pos = r.percent
        	})

            $(this.el).html(Templates.decisionPanelTemplate(decisionPublicDisplay));

            return this;
        }
    });

    return new V()
 }
