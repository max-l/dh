
/* CreateDecisionWizard
 *  1: AdminView / DecisionForm  !(clickNext : Invite participants)
 *  2: AdminView / ParticipantsView(editable=true) !(clickNext : Start Election)
 *  ---> DecisionWidget
 *
 * DecisionWidget
 *   tab1: DecisionPublicView + ParticipantsView(editable=false)
 *   tab2: BallotView
 *   tab3: AdminView (DecisionForm + ParticipantsView(editable=true)) (if owner)
 */

CollectionZ = Backbone.Collection.extend({
	model: Backbone.Model,
	sync: function() {return false},
    initialize: function() {
    	this.superCreate = this.create
    	this.create = function(o, atts) {
    		if(this.model.extend)
    		  o = new this.model(o, atts);
    		o.sync = this.sync;
    		this.superCreate(o)
    	}
    },
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
        
CreateDecisionWizard = function() {

	
	
	var createPopup = function (title, content, selector) {
		return _.once(function() {
		  
		  var pop = $(this.el).find(selector).popover({
		     placement:'right',
		     title: title,
	         trigger: 'manual',
	         content: content
          })

          //pop.show = function() {pop.popover('show')}
		  //pop.hide = function() {pop.popover('hide')}
          return pop.data('popover')
        })
	}
	
	var validateTitle = function(title) {
		return title.length > 3
	}

    var _createChoicesListView = function() {

            ChoiceView = Backbone.View.extend({
                events: {
                    "click i.icon-remove": function() {
        	            this.model.destroy();
        	        },
                    "blur input" : function(e) {
                    	var txt = $(e.currentTarget).val();
                        if(txt != this.model.get('title')) {
                          this.model.set('title', txt);
                          this.model.save()
                        }
                    }
                },
    	    	initialize: function() {
                    this.model.bind('change', this.render, this);
                    this.model.bind('destroy', this.remove, this)
    	        },
    	        render: function() {
    	            $(this.el).html(Templates.choiceTemplate(this.model.toJSON()));
    	            return this;
    	        },
    	        remove: function() {
    	            $(this.el).remove();
    	        }
            });

            ChoicesListView = DynListView.extend({
              model: new CollectionZ(),
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
            	$(this.el).html(
            	  $('<input type="text" placeholder="Enter choices"></input><ul></ul>')
            	)
            	return this
              },
              events: {
            	"keypress input:first-child" : function(e) {
                   if (e.keyCode != 13) return;
                   var text = this._textInput().val();
                   if(! text) return;
                   this.model.create({title: text})
                   this._textInput().val('');
                 }  
              },
              _textInput: _.once(function() {
            	  return this.$("input:first-child")
              })
            });

            return new ChoicesListView()
    }

    var V = Backbone.View.extend({
    	_titleWarning: createPopup('', "Enter a meaningful title", 'input[name=title]'),
        _choicesWarning: createPopup('', "An vote with a single choice isn't a choice !", '#choiceList input:first-child'),
        _choicesListView: _createChoicesListView(),
    	events: {
        	"click #nextChoices" : function() {
    	        if(validateTitle($('input[name=title]').val()))
    	          this.enableOrDisable('a[href=#choices]', true)
    	        else
    			  this._titleWarning().show()
            },
        	"click #nextParticipants" : function() {
            	//FB's apprequest model requires to have a persisted Decision b4 
            	//any invitation is sent so now is the time to save :
            	
            	if(this._choicesListView.model.length < 2)
            		this._choicesWarning().show()
            	else {
            	  var decision = this.model;
            	  var zis = this;
            	  if(decision.isNew()) {
                	  decision.save(null, {
                		  success: function() {
                		      decision.sync = Backbone.sync;
                	          zis._choicesListView.model.persist("/dec/bulkalternatives/" + decision.id, '/dec/alternatives/'+ decision.id)
                	          zis.enableOrDisable('a[href=#participants]', true)
                	      }
                	  })
                  }
                  else zis.enableOrDisable('a[href=#participants]', true)
            	}
            },
        	"click #nextFinish" : function() {
            	$(this.el).modal('hide')
            	$(this.el).remove()
            },
            "keyup input[name=title]" : function(e) { 
    	        if(validateTitle($(e.currentTarget).val()))
    	           this._titleWarning().hide()
            },
            "click #closeWizard": function() {
            	this._titleWarning().hide()
            }
        },
    	model: new Decision({
    	    sync: function() {return false}
    	}),
    	initialize: function() {
        	var v = this.render()
        	$('body').append(v)
        	//this.delegateEvents();
        	$('#createDecisionWizard').modal('show')
        	var zis = this;
        	
        	zis._choicesListView.model.on('add', function(){
        		
        		var w = zis._choicesListView.model.length
        		debugger;
        		
        		if(zis._choicesListView.model.length > 1)
        			zis._choicesWarning().hide()
        	},this)


        	this.enableOrDisable('a[href=#choices]', false)
        	this.enableOrDisable('a[href=#participants]', false)
        	this.enableOrDisable('a[href=#finish]', false)
    	},
    	render: function() {
    		var e = $(this.el)
    		e.html($(Templates.createDecisionWizardTemplate()))

    		e.find('#choiceList').html(this._choicesListView.render().el)

    		return e
    	},
        enableOrDisable: function(tabId, enable) {
    	    var a = $(this.el).find(tabId)
    	    if(a.length != 1) throw 'anchor does not exist'
    	    a.prop('disabled', ! enable)
    	    if(enable) a.trigger('click')
    	}
    })
    return new V()
}()
