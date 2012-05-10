
    window.Todo = Backbone.Model.extend({
        defaults: function() {
            return {id:null};
        }
    });

    window.TodoView = Backbone.View.extend({

        tagName: "li",

        _templateForItem: _.template($('#item-template').html()),

        // The DOM events specific to an item.
        events: {
            "click i.icon-remove": "clear",
            "blur input" : "change"
        },

        // The TodoView listens for changes to its model, re-rendering.
        initialize: function() {
            this.model.bind('change', this.render, this);
            this.model.bind('destroy', this.remove, this);
        },

        // Re-render the contents of the todo item.
        render: function() {
            var dt = this.model.toJSON();
            console.log(dt);
            $(this.el).html(this._templateForItem(dt));
            return this	
        },
        
        change: function() {
	       //if(this.model.hasChanged("text"))
	       this.model.save();
        },

        remove: function() {
            console.log('a2')
            $(this.el).remove();
        },

        clear: function() {
            console.log('a1')
            console.log(this.model.cid)
            this.model.destroy();
        }

    });

    window.TodoList = Backbone.Collection.extend({
        model: Todo,
        // Save all of the todo items under the `"todos"` namespace.
        //localStorage: new Store("todos")
        url: 'http://localhost:9000/td'
    });

    window.AppView = Backbone.View.extend({

        statsTemplate: _.template($('#stats-template').html()),
        //events: {"keypress #todoapp input": "createOnEnter"},

        initialize: function() {

        	//console.log(this.todoList);
        	var i = $(this.el).find("input").first();
        	var md = this.model;
        	
        	i.keypress(function(e) {
                
                if (e.keyCode != 13) return;
                var txt = i.val();
                if(! txt) return;
                
                md.create({
                    text: txt
                });
                i.val('');
            });

            this.model.bind('add', this.addOne, this);
            this.model.bind('reset', this.addAll, this);
            this.model.bind('all', this.render, this);
            this.model.fetch();
        },

        // Re-rendering the App just means refreshing the statistics -- the rest
        // of the app doesn't change.
        render: function() {
            this.$('#todo-stats').html(this.statsTemplate({
                total: this.model.length
            }));
        },

        // Add a single todo item to the list by creating a view for it, and
        // appending its element to the `<ul>`.
        addOne: function(todo) {
            var view = new TodoView({model: todo});
            var ul = this.$("ul");
            ul.prepend(view.render().el);
        },

        // Add all items in the **Todos** collection at once.
        addAll: function() {
        	this.model.each(this.addOne);
        }
    });

    // Finally, we kick things off by creating the **App**.
    window.App = new AppView({model: new TodoList(), el: $("#todoapp")});
