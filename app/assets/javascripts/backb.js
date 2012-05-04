
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
            $(this.el).html(this._templateForItem(dt));

            var text = this.model.get('text');
            this.$('.todo-text').text(text);
            this.input = this.$('.todo-input');
            this.input.val(text);
            return this;
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

    window.Todos = new TodoList;

    // The Application
    // ---------------
    // Our overall **AppView** is the top-level piece of UI.
    window.AppView = Backbone.View.extend({

        // Instead of generating a new element, bind to the existing skeleton of
        // the App already present in the HTML.
        el: $("#todoapp"),

        // Our template for the line of statistics at the bottom of the app.
        statsTemplate: _.template($('#stats-template').html()),

        // Delegated events for creating new items, and clearing completed ones.
        events: {
            "keypress #new-todo": "createOnEnter"
        },
        inputField: this.$("#new-todo"),

        // At initialization we bind to the relevant events on the `Todos`
        // collection, when items are added or changed. Kick things off by
        // loading any preexisting todos that might be saved in *localStorage*.
        initialize: function() {

            Todos.bind('add', this.addOne, this);
            Todos.bind('reset', this.addAll, this);
            Todos.bind('all', this.render, this);
            Todos.fetch();
        },

        // Re-rendering the App just means refreshing the statistics -- the rest
        // of the app doesn't change.
        render: function() {
            this.$('#todo-stats').html(this.statsTemplate({
                total: Todos.length
            }));
        },

        // Add a single todo item to the list by creating a view for it, and
        // appending its element to the `<ul>`.
        addOne: function(todo) {
            var view = new TodoView({model: todo});
            this.$("#todo-list").prepend(view.render().el);
        },

        // Add all items in the **Todos** collection at once.
        addAll: function() {
            Todos.each(this.addOne);
        },

        // If you hit return in the main input field, and there is text to save,
        // create new **Todo** model persisting it to *localStorage*.
        createOnEnter: function(e) {
            //console.log('zaza')
            var text = this.inputField.val();
            if (!text || e.keyCode != 13) return;
            Todos.create({
                text: text
            });
            this.inputField.val('');
        }
    });

    // Finally, we kick things off by creating the **App**.
    window.App = new AppView;
