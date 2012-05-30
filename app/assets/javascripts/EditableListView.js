/**
 * 
 * options : {
 *   collectionModel: a Backbone.Collection that will be assigned to the ChoiceListView
 *   elementModel : the Backbone.Model for elements
 *   elementFieldName : the field name (of the element model) that this choice lists edits
 * }
 */ 

EditableListView = function(options) {

    var _collectionModel = options.collectionModel || Backbone.Collection;
    var _elementFieldName = options.elementFieldName 
    var _newChoiceCreationFieldHtml = options.newChoiceCreationFieldHtml || '<input type="text" placeholder="Enter choices"></input>';
    var _choiceTemplate = options.choiceTemplate || Templates.choiceTemplate;

    if(! _elementFieldName) throw new Error('missing elementFieldName option');

    var ChoiceView = Backbone.View.extend({
        events: {
            "click i.icon-remove": function() {
	            this.model.destroy();
	        },
            "blur input" : function(e) {
            	var txt = $(e.currentTarget).val();
                if(txt != this.model.get(_elementFieldName)) {
                  this.model.set(_elementFieldName, txt);
                  this.model.save()
                }
            }
        },
    	initialize: function() {
            this.model.bind('change', this.render, this);
            this.model.bind('destroy', this.remove, this)
        },
        render: function() {
            $(this.el).html(_choiceTemplate(this.model.toJSON()));
            return this;
        },
        remove: function() {
            $(this.el).remove();
        }
    });

    var V = Backbone.View.extend({
      model: options.collectionModel,
      initialize: function() {
        this.model.on('add', this.addOne, this);
        this.model.on('reset', this.addAll, this);
      },
      addAll: function() {

      	var ul = this.$("ul");
      	ul.empty();
      	this.model.each(this.addOne, this);
      },
      addOne: function(listElementModel) {

          var view = new ChoiceView({model: listElementModel});
          var ul = this.$("ul");
          ul.prepend(view.render().el);
      },
      render: function() {

    	$(this.el).html($(_newChoiceCreationFieldHtml))
    	this._textInput = this.$("input:first-child");
    	var zis = this;
    	this._textInput.keypress(function(e) {
           if (e.keyCode != 13) return;
           var text = zis._textInput.val();
           if(! text) return;
           var newObject = {};
           newObject[_elementFieldName] = text
           zis.model.create(newObject)
           zis._textInput.val('');
         })

    	$(this.el).append($('<ul></ul>'))
    	return this
      }
    });

    return new V()
}
