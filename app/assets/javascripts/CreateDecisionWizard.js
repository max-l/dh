
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


CreateDecisionWizard = function() {

	
	
	var createPopup = function (title, content, selector) {
		return _.once(function() {
		  
		  var pop = $(this.el).find(selector).popover({
		     placement:'right',
		     title: title,
	         trigger: 'manual',
	         content: content
          })
          return pop.data('popover')
        })
	}
	
	var validateTitle = function(title) {
		return title.length > 3
	}

    var V = Backbone.View.extend({
    	_titleWarning: createPopup('', "Enter a meaningful title", 'input[name=title]'),
        _choicesWarning: createPopup('', "Give electors more than one choice !", '#choiceList input:first-child'),
        _choicesListView: new EditableListView({
        	collectionModel: new InitiallyTransientCollection(),
            elementFieldName: 'title'
        }),
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
        	$('#createDecisionWizard').modal('show')
        	var zis = this;
        	zis._choicesListView.model.on('add', function() {
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
