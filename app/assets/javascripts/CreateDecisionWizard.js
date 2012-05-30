
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
	
	var popups = new Array();
	
	var createPopup = function (el, title, content, selector) {

		  var create = function() {
    		  var i = $(el).find(selector);
    		  var pop = i.popover({
    		     placement:'right',
    		     title: title,
    	         trigger: 'manual',
    	         content: content,
    		     template: '<div class="popover"><div class="arrow"></div><div class="popover-inner"><div class="popover-content"><p></p></div></div></div>'
              })
             return pop
    	 }
		  
         return {
        	  show: function() {
        	    this.pop = create()
        	    this.p = this.pop.data('popover');
                popups.push(this)
        	    this.p.show()
        	  },
        	  hide: function() {
        		this.p.hide()
        		popups = _.without(popups, this)
        		this.pop.removeData()
        	  }
         }
	}
	
	var hideWarnings = function() {
	    _.forEach(popups, function(w){ w.hide()})
	}
	
	var validateTitle = function(title) {
		return title.length > 3
	}
	
	var TITLE_SELECTOR = 'input[name=title]';

    var V = Backbone.View.extend({
        _choicesListView: new EditableListView({
        	collectionModel: new InitiallyTransientCollection(),
            elementFieldName: 'title'
        }),
    	events: {
    	    "click [href=#decisionTitle]": function() {
    	       hideWarnings()
            },
        	"click #nextChoices" : function() {
    	        if(validateTitle($(TITLE_SELECTOR).val()))
    	          this.enableOrDisable('a[href=#choices]', true)
    	        else
    			  this._titleWarning.show()
            },
            'keypress input[name=title]' : function(e) {
                if (e.keyCode != 13) return;
                this.$('#nextChoices').trigger('click')
            },
        	"click #nextParticipants" : function() {
            	//FB's apprequest model requires to have a persisted Decision b4 
            	//any invitation is sent so now is the time to save :
            	
            	if(this._choicesListView.model.length < 2)
            		this._choicesWarning.show()
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
    	           hideWarnings()
            },
            'blur input[name=title]' : function() {
                if(! this.model.isNew())
                	this.model.save()
            },
            "click #closeWizard": function() {
            	hideWarnings()
            }
        },
    	model: new Decision({
    	    sync: function() {return false}
    	}),
    	initialize: function() {
        	var v = this.render()
        	$('body').append(v)
        	
        	
    	    this._titleWarning = createPopup(this.el,'', "Enter a meaningful title", TITLE_SELECTOR),
        	this._choicesWarning = createPopup(this.el,'', "Give voters at least two choices !", '#choiceList input:first-child'),
        	this._duplicateChoiceWarning = createPopup(this.el,'', "This choice already exists", '#choiceList input:first-child'),
        
        	this.modal = $('#createDecisionWizard').modal({backdrop: 'static'}).data('modal')
        	var zis = this;
        	
        	this.modal.show()
        	
        	this._choicesListView.duplicateHandler = function(isDuplicate, text) {
        		if(isDuplicate) zis._duplicateChoiceWarning.show()
        		else hideWarnings()
        	}
        	
        	zis._choicesListView.model.on('add', function() {
        		if(zis._choicesListView.model.length > 1)
        			hideWarnings()
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
