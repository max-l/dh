

function interpolateColors000(x1, x2, percent) {
	var red1 = x1 >> 16;
	var green1 = (x1 >> 8) & 0xFF;
	var blue1  = x1 & 0xFF;

	var red2 = x2 >> 16;
	var green2 = (x2 >> 8) & 0xFF;
	var blue2  = x2 & 0xFF;

	var time = percent; //0.3 // This should be between 0 and 1

	return {
		r:time * red1 + (1-time) * red2,
	    g:time * green1 + (1-time) * green2,
	    b:time * blue1 + (1-time) * blue2
	}
}

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

        	var c1 = 'ff0905'; // 0%   255,9,5 
        	var c2 = 'fff45e'; // 45%  255,244,94
        	var c3 = 'fff45e'; // 55%
        	var c4 = '2db70e'; // 100% 45,183,14
        	_.each(decisionPublicDisplay.results, function(r) {
        		
        	var r1 = new Rainbow(c1, c2);
        	r1.setNumberRange(0, 45);
        	var r2 = new Rainbow(c2, c3);
        	r2.setNumberRange(45, 55);
        	var r3 = new Rainbow(c3, c4);
        	r3.setNumberRange(55, 100);
        	
        		if(r.percent <= 45) {
        		    //r.color = r1.colorAt(r.percent)
        			r.color = interpolateColorsR(z1, z2, 0, 45, r.percent)
        		}
        		else if(r.percent <= 55) {
        		  r.color = interpolateColorsR(z2, z2, 0, 55, r.percent)
        		}
        		else {
        		  r.color = interpolateColorsR(z2, z3, 0, 100, r.percent)
        		}
        	})
        	
            $(this.el).html(Templates.decisionPanelTemplate(decisionPublicDisplay));

            return this;
        }
    });

    return new V()
 }
