
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
	
	var TITLE_SELECTOR = 'input[name=title]';

    var V = Backbone.View.extend({
    	_titleWarning: createPopup('', "Enter a meaningful title", TITLE_SELECTOR),
        _choicesWarning: createPopup('', "Give electors more than one choice !", '#choiceList input:first-child'),
        _choicesListView: new EditableListView({
        	collectionModel: new InitiallyTransientCollection(),
            elementFieldName: 'title'
        }),
    	events: {
        	"click #nextChoices" : function() {
    	        if(validateTitle($(TITLE_SELECTOR).val()))
    	          this.enableOrDisable('a[href=#choices]', true)
    	        else
    			  this._titleWarning().show()
            },
            'keypress input[name=title]' : function(e) {
                if (e.keyCode != 13) return;
                this.$('#nextChoices').trigger('click')
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
                	          zis._participantView = new ParticipantsView(decision.id, "You are invited to vote on " + decision.get('title'));
                	          zis.$('#participantsList').html(zis._participantView.render().el);
                	          // we enable the next 2 tabs : 
                	          zis.enableOrDisable('a[href=#participants]', true)
                	          zis.$('a[href=#finish]').prop('disabled', false)
                	      }
                	  })
                  }
                  else zis.enableOrDisable('a[href=#participants]', true)
            	}
            },
        	"click #nextFinish" : function(btn) {
            	this.$("a[href=#finish]").trigger('click')
            },
            "click #startVoting": function() {
            	this.modal.hide()
            	$(this.el).remove()
            },
            "click #gotoDecisionWidget": function() {
            	this.modal.hide()
            	$(this.el).remove()
            },
            'keyup input[name=title]' : function(e) {
            	var t = $(e.currentTarget).val();
                this.model.set('title', t);
    	        if(validateTitle(t))
    	           this._titleWarning().hide()
            },
            'blur input[name=title]' : function() {
                if(! this.model.isNew())
                	this.model.save()
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
        	this.modal = $('#createDecisionWizard').modal({backdrop: 'static'}).data('modal')
        	var zis = this;
        	
        	this.modal.show()
        	
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
    		
    		//this.$(TITLE_SELECTOR)[0].selectionStart = 0; //focus();

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
}
