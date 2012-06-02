
/* CreateDecisionWizard
 *  1: AdminView / DecisionForm  !(clickNext : Invite participants)
 *  2: AdminView / ParticipantsView(editable=true) !(clickNext : Start Election)
 *  ---> DecisionWidget
 *
 * DecisionWidget
 *   tab1: DecisionPublicView + ParticipantsView(editable=false)
 *   tab2: BallotView
 *   tab3: AdminView (DecisionForm + ParticipantsView(editable=true)) (if owner)
 *   
 *   
 *   
 *   Wizard
 *   
 *   
 *   
 *   The decision can be linked to your Facebook account, your email adress, in which case
 *   DecisionHub will send invitations. 
 *   
 *   Who can vote on this decision ?
 *   
 *   
 *   - I want to send the invitations myself
 *   
 *   step1 : What is the vote about ?
 *          (title + choices)
 *   step2 : Who will vote ?
 *   - only the participants that I invite
 *     (email +| facebookLogin)
 *   - anyone who has a link
 *   
 *    name || facebookLogin || email
 *   step3 : invitations
 *   
 *   
 *   
 */


CreateDecisionWizard = function() {
	
    
    
    var facebookNotEnabledCaption = "Enable invitations by Facebook (optional)"

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
        validatorsBeforeTabs: {
    	  "#step2": function(zis) {
    	     
    	     var titleOk = validateTitle(zis.$(TITLE_SELECTOR).val());
    	     var choicesOk = zis._choicesListView.model.length >= 2
             if(titleOk && choicesOk) zis.enableTab('#step2')
             
             if(!choicesOk) zis._choicesWarning.show()
             if(!titleOk) zis._titleWarning.show()
			 return titleOk && choicesOk
          },
    	  "#step3": function(zis) {

              if(zis.$("a[href=#voteOnInvitationOnly]").hasClass("active")) {
            	  var facebookEnabled = zis.$('#enableFacebook').hasClass("active")
            	  var em = zis.$('#emailAddress').val()
            	  var hasValidEmail = validateEmail(em)
            	  if(!(facebookEnabled || hasValidEmail)) zis._needFBOrEmailWarning.show()
            	  else return true
              }
              else {
            	  zis.enableTab('#step3')
            	  return true
              }
          }
        },
    	events: {
        	//Toggle options like a radio group 
        	"click a[href=#voteOnInvitationOnly]": function(a) {
        		if(!$(a.currentTarget).hasClass("active")) {
        		    this._voteOnInvitationOnly.show()
        		    this._anyOneCanVote.hide()        			
        		}
        	    $(a.currentTarget).addClass("active")
        	    this.$("a[href=#anyOneCanVote]").removeClass("active")
        	    hideWarnings()
            },
        	"click a[href=#anyOneCanVote]": function(a) {
            	if(!$(a.currentTarget).hasClass("active")) {
        		    this._voteOnInvitationOnly.hide()
        		    this._anyOneCanVote.show()            		
            	}
            	$(a.currentTarget).addClass("active")
            	this.$("a[href=#voteOnInvitationOnly]").removeClass("active")
            	hideWarnings()
            },
            //set 'Next' button to trigger tab switch
    	    "click a[goto]": function(a) {
    	       var tabId = $(a.currentTarget).attr('goto');
    	       var btn = this.$('a[href=' + tabId + "]")
    	       var v = this.validatorsBeforeTabs[tabId]
    	       if(v && v(this)) {
    	    	 hideWarnings()
    	         btn.trigger('click')
    	       }
            },
            //always hide warnings on a tab switch
    	    "click [href^=#step]": function() {
    	       hideWarnings()
            },
            'keypress input[name=title]' : function(e) {
                if (e.keyCode != 13) return;
                this.$('#nextChoices').trigger('click')
            },
            'keyup input[id=yourName1]' : function() {
            	//debugger;
                this.$("#yourName2").val(this.$("#yourName1").val())
            },            
            'keyup input[id=yourName2]' : function() {
            	//debugger;
            	this.$("#yourName1").val(this.$("#yourName2").val())
            },            
        	"click #nextYourName" : function() {
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
                	          zis.enableOrDisable('a[href=#yourName]', true)
                	          zis.$('a[href=#finish]').prop('disabled', false)
                	      }
                	  })
                  }
                  else zis.enableOrDisable('a[href=#yourName]', true)
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
        	this.render()

    	    this._titleWarning = createPopup(this.el,'', "Enter a meaningful title", TITLE_SELECTOR),
        	this._choicesWarning = createPopup(this.el,'', "Give voters at least two choices !", '#choiceList input:first-child'),
        	this._duplicateChoiceWarning = createPopup(this.el,'', "This choice already exists", '#choiceList input:first-child'),
        	
        	this._needFBOrEmailWarning = createPopup(this.el,'', "You need to enter an email address, enable Facebook invatations, or select 'Anyone can join'", '#voteOnInvitationOnly'),
        
        	this.modal = this.$('#createDecisionWizard').modal({backdrop: 'static'}).data('modal')
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


        	//this.disableTab("#step2")
        	this.disableTab("#step3")
        	this.disableTab("#step4")
    	},
    	render: function() {
    		var zis = this;
    		$(this.el).html($(Templates.createDecisionWizardTemplate()))
    		//$('#createDecisionWizard')
    		//var e = $(this.el)
    		//e.html()

    		this.$('#choiceList').html(this._choicesListView.render().el)
    		$('body').append(this.el)

//    		this.$("#anyOneCanVote").on('hidden', function(ev) {
//    			if(zis.$('[href=#anyOneCanVote]').hasClass('active')) ev.stopImmediatePropagation()
//    		})
    		//this.$(TITLE_SELECTOR)[0].selectionStart = 0; //focus();

    		this._voteOnInvitationOnly = this.$('#voteOnInvitationOnly').collapse({toggle:false}).data('collapse')
    		this._anyOneCanVote = this.$('#anyOneCanVote').collapse().data('collapse')
    		
    		return this
    	},
        enableOrDisable: function(tabId, enable) {
    	    var a = $(this.el).find(tabId)
    	    if(a.length != 1) throw 'anchor does not exist'
    	    a.prop('disabled', ! enable)
    	    if(enable) a.trigger('click')
    	},
        disableTab: function(tabId) {
    	    var a = $(this.el).find('a[href=' + tabId+ ']')
    	    if(a.length != 1) throw 'anchor does not exist'
    	    a.prop('disabled', true)
    	},    	
        enableTab: function(tabId) {
    	    var a = $(this.el).find('a[href=' + tabId+ ']')
    	    if(a.length != 1) throw 'anchor does not exist'
    	    a.prop('disabled', false)
    	},
        loggedInFacebook: function(meResp, fbAuthResponse) {
        	var zis = this;
        	if(fbAuthResponse) {
                $.ajax({
                    type: 'POST',
                    url: "/loginWithFacebookToken",
                    data: JSON.stringify(fbAuthResponse),
                    success: function() {
                	  zis.loggedInFacebookAndDecisionHub(meResp, fbAuthResponse)
                    },
                    error: function() {
                    	zis.$('#enableFacebook').text(facebookNotEnabledCaption)
                    },
                    contentType: "application/json; charset=utf-8",
                    dataType: 'json'
                })
            }
        },
        loggedInFacebookAndDecisionHub: function(meResp, fbAuthResponse) {
      	    this.$('#enableFacebook').text('Facebook invitations via ' + meResp.name)
      	    this.$('#enableFacebook').addClass("active")
        },
        ready: function() {
        	
        },
        loggedOutFacebook: function() {
        	this.$('#enableFacebook').text(facebookNotEnabledCaption)
        }
    })
    
    var validateEmail = function (email) { 
      // http://stackoverflow.com/a/46181/11236
        var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(email);
    }    
    return new V()
}
