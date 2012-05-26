
function createDecisionViewPanel(rootElement) {
	

        function rgbColor(rc, gc, bc) {
        	return {r:rc, g:gc, b:bc,
              asString: function() {
        	    return "rgb(" + this.r + "," + this.g + "," + this.b + ")"
              },
              inverse: function() {
            	return rgbColor(255 - this.r, 255 - this.g, 255 - this.b)
              }
            }
        }
        
        function interpolateColors(x1, x2, time) {
        	return rgbColor(
        		   Math.round(time * x1.r + (1-time) * x2.r),
        	       Math.round(time * x1.g + (1-time) * x2.g),
        	       Math.round(time * x1.b + (1-time) * x2.b))
        }
        
        function interpolateColorsR(x1, x2, min, max, n) {
        	var p = max - min
        	var time = 1 - (n / p);
        	return interpolateColors(x1, x2, time)
        }
	
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

        	var z1 = rgbColor(255, 9,   5)
        	var z2 = rgbColor(255, 244, 94)
        	var z3 = rgbColor(45,  183, 14)

        	_.each(decisionPublicDisplay.results, function(r) {

        		if(r.percent <= 45)
        		  r.rgbColor = interpolateColorsR(z1, z2, 0, 45, r.percent).asString()
        		else if(r.percent <= 55)
        		  r.rgbColor =  z2.asString()
        		else
        		  r.rgbColor = interpolateColorsR(z2, z3, 0, 100, r.percent).asString()

        		if(r.percent < 40 || r.percent > 60)
        		  r.scoreColor = "white"
        		else
        		  r.scoreColor =  "black"

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
