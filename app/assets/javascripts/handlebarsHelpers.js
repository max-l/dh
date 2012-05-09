

Handlebars.registerHelper('applyTemplate', function(subTemplateId,ctx){

    var subTemplate =  Handlebars.compile($('#' + subTemplateId).html());
    var innerContent = ctx.fn({});
    var subTemplateArgs = _.extend({}, 
      ctx.hash, 
      {content: new Handlebars.SafeString(innerContent)});

    return subTemplate(subTemplateArgs)
});