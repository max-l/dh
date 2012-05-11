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

        initialize: function() {

	        var zis = this;

            var i = $(this.el).find("input").first();
            
            i.keypress(function(e) {

                if (e.keyCode != 13) return;
                var text = i.val();
                if(! text) return;
                
                zis.createNewElement(zis.model, text);
                i.val('');
            });

            this.model.bind('add', this.addOne, this);
            this.model.bind('all', this.render, this);
            this.model.bind('reset', this.addAll, this);
            this.model.fetch();
        },
        addAll: function() {
        	this.model.each(this.addOne, this);
        },
        addOne: function(e) {
            var view = this.createElementView(e);
            var ul = this.$("ul");
            ul.prepend(view.render().el);
        }
    });
