
Decision = Backbone.Model.extend({
    urlRoot: "/dec"
});

ChoiceList = function(decisionId) {
  var M = Backbone.Collection.extend({
     model: Backbone.Model,
     url: "/dec/alternatives/" + decisionId
  })
  return new M()
}

DecisionPublicInfo = Backbone.Model.extend({
	url: function() {return '/decision/' + this.id}
});


BallotModel = Backbone.Model.extend({
    url: function() {return '/ballot/' + this.id}
});

/**
 * [{decisionId:x1}, {decisionId:x2}, ...]
 */
MyDecisionIds = Backbone.Collection.extend({
    model: Backbone.Model,
    url: "/myDecisionIds"
});

InitiallyTransientCollection = Backbone.Collection.extend({
	model: Backbone.Model,
	sync: function() {return false},
    initialize: function() {
    	this.superCreate = this.create
    	this.create = function(o, atts) {
    		if(this.model.extend) //model is a class (not an instance...)
    		  o = new this.model(o, atts);
    		o.sync = this.sync;
    		this.superCreate(o)
    	}
    },
    /*
     * bulkSaveUrl: for posting all ellements, it must return a JSON list of Ids, 
     * url: the url attribute of elements, after this collection becomes persistent 
     */
    persist: function(bulkSaveUrl, url) {
	    var zis = this;
        $.ajax({
          type: 'POST', url: bulkSaveUrl,
          data: JSON.stringify(this.toJSON()),
          success: function(newIds) {
  		      zis.sync = Backbone.sync;
  		      zis.url = url;
  		      var c = 0;
		      zis.forEach(function(m) {
        		  m.sync = Backbone.sync;
        		  m.id = newIds[c];
        		  c = c + 1
        	  });
          },
          error: function() {},
          contentType: "application/json; charset=utf-8",
          dataType: 'json'
        })
   }    
})
