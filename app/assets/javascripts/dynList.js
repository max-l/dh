    DynListElement = Backbone.Model.extend({
        //defaults: function() {return {id:null}}
    });

    DynListElementView = Backbone.View.extend({
        events: {
            "click i.icon-remove": "clear",
            "blur input" : "change"
        },
        tagName: "li",
        compiledTemplate: undefined,
        initialize: function() {
            this.model.bind('change', this.render, this);
            this.model.bind('destroy', this.remove, this)
        },
        render: function() {
            $(this.el).html(this.compiledTemplate(this.model.toJSON()));
            return this;
        },
        change: function(e) {
        	var txt = $(e.currentTarget).val();
        	
        	if(txt != this.model.get('title')) {
        	  this.model.set('title', txt);
              this.model.save()
        	}
        },
        remove: function() {
            $(this.el).remove();
        },
        clear: function() {
            this.model.destroy();
        }
    });

    DynList = Backbone.Collection.extend({
        model: DynListElement
    });

    DynListView = Backbone.View.extend({

        render: function() {

	        var zis = this;

            var i = $(this.el).find("input").first();

            if($(this.el).length != 1) throw "!!!!!!!!!!!!!"

            
            i.keypress(function(e) {

                if (e.keyCode != 13) return;
                var text = i.val();
                if(! text) return;
                
                zis.createNewElement(zis.model, text);
                i.val('');
            });

            //this.model.fetch();
        },
    	setModel : function(list) {
        	this.model = list;
            this.model.on('add', this.addOne, this);
            this.model.on('all', this.render, this);
            this.model.on('reset', this.addAll, this);
            //this.model.on('change', this.render, this);
        },
        addAll: function() {
        	var ul = this.$("ul");
        	ul.empty();
        	this.model.each(this.addOne, this);
        },
        addOne: function(e) {
            var view = this.createElementView(e);
            var ul = this.$("ul");
            ul.prepend(view.render().el);
        }
    });
