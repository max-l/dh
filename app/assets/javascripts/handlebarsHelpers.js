!function($) {

    var handlebarsHelpersCache = {};

    Handlebars.registerHelper('applyTemplate', function(subTemplateId,ctx){

    	var subTemplate = handlebarsHelpersCache[subTemplateId];
    	if(! subTemplate) {
    		subTemplate = Handlebars.compile($('#' + subTemplateId).html());
    		handlebarsHelpersCache[subTemplateId] = subTemplate
    	}

        var innerContent = ctx.fn(this);
        //var innerContent = ctx.fn({});
        var subTemplateArgs = _.extend({}, 
          ctx.hash, 
          {content: new Handlebars.SafeString(innerContent)});

        return subTemplate(subTemplateArgs)
    })
    
    
    Handlebars.registerHelper('ifNot', function(conditional, options) {
      if(! conditional) {
        return options.fn(this);
      } else {
        return options.inverse(this);
      }
    });

}(window.jQuery);
