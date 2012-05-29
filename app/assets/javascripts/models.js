
Decision = Backbone.Model.extend({
    defaults: function() {
        return {title: ""}
    },
    choiceList: _.once(function() {
        var ChoiceList = Backbone.Collection.extend({
           model: Backbone.Model,
           url: "/dec/alternatives/" + this.id
        });
        return new ChoiceList({id: this.id})
    }),
    urlRoot: "/dec",
    getFBParticipants: _.once(function() {
        FBParticipant = Backbone.Model.extend({});

        FBParticipantsList = Backbone.Collection.extend({
            model: FBParticipant,
            url: "/dec/participants/" + this.id
        });

    	return new FBParticipantsList()
    })
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
