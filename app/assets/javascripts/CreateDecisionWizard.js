
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
	
    var linkTofacebookButtonCaption = function(fbName) {
    	if(fbName) return "Decision will be linked to your Facebook account (" + fbName + ")"
    	return "Link this decision to your Facebook account"
    }
    
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
        	    if(this.pop && this.pop.data('popover')) return
 	            this.pop = create()
	            this.p = this.pop.data('popover');
        	    if(!this.p) throw Error("Bad selector " + selector)
        	    popups.push(this)
        	    this.p.show()
        	  },
        	  hide: function() {
        		if(! this.p) return
        		this.p.hide()
        		this.pop.removeData()
        		popups = _.without(popups, this)
        	  }
         }
	}
	

	var hideWarnings = function() {
	    _.forEach(popups, function(w){ w.hide()})
	}
	
	var TITLE_SELECTOR = 'input[name=title]';

    var V = Backbone.View.extend({
        _choicesListView: new EditableListView({
        	collectionModel: new InitiallyTransientCollection(),
            elementFieldName: 'title',
            newChoiceCreationFieldHtml: '<input id="choiceInput" type="text" placeholder="Enter choice, and press enter"></input>'
        }),
        validatorsBeforeTabs: {
    	  "#step2": function(zis, onValid) {
    	     var titleOk = zis._titleOk()
    	     var choicesOk = zis._choicesOk()
    	     
             if(titleOk && choicesOk) {
         	     zis.enableTab("#step3-public")
        	     zis.enableTab("#step3-private")
            	 onValid()
             } 
          },
    	  "#step4-private": function(zis, onValid) {
        	  if(zis.isLinkedToFacebook()) {
        		  if(zis._enableFacebookOk()) {

        			  zis.$('#privateConfirmationMessage').html(
        				  $("<p>The decision <b>" + zis.model.get('title') + 
        					"</b> will be tied to your Facebook account. You can access it here http://apps.facebook.com/equivote" +
        					" or by logging with facebook here : here http://equivote.net</p>")
        			  )

        			  onValid()
        		  }
        	  }
        	  else {
        		  if(zis._ownerEmailOk()) {
        			  zis.$('.confirmationMessage').text(
        				  "You will receive a confirmation link at " + zis.model.get('ownerEmail')+
        				  " you need to follow this link to activate this Decision")
        			  onValid()
        		  }
        	  }
          }
        },
        isLinkedToFacebook: function() {
        	return this.$("a[href=#linkToFacebook]").hasClass("active")
        },
    	events: {
        	//Toggle options like a radio group 
        	"click #registrationPrivate": function(a) {
        	    this.model.set('isPublic', false)
            },
        	"click #registrationPublic": function(a) {
            	this.model.set('isPublic', true)
            },
        	"click a[href=#linkToFacebook]": function(a) {
        		if(!$(a.currentTarget).hasClass("active")) {
        		    this._linkToFacebook.show()
        		    this._linkToEmail.hide()        			
        		}
        	    $(a.currentTarget).addClass("active")
        	    this.$("a[href=#linkToEmail]").removeClass("active")
        	    hideWarnings()
            },
        	"click a[href=#linkToEmail]": function(a) {
            	if(!$(a.currentTarget).hasClass("active")) {
        		    this._linkToFacebook.hide()
        		    this._linkToEmail.show()
        		    this.model.set('fbAuth', null)
            	}
            	$(a.currentTarget).addClass("active")
            	this.$("a[href=#linkToFacebook]").removeClass("active")
            	hideWarnings()
            },
            "click #step3": function() {
              if(this.model.get('isPublic')) 
            	  this.$('[href=#step3-public]').trigger('click')
              else 
            	  this.$('[href=#step3-private]').trigger('click')
              
            },
            //set 'Next' button to trigger tab switch
    	    "click a[goto]": function(a) {
               if(! this._inhibitGotoTab) {
        	       var tabId = $(a.currentTarget).attr('goto');
        	       var btn = this.$('a[href=' + tabId + "]")
        	       this.gotoTab(tabId, btn)
        	   }
            },/*
    	    "click a[href^=#step]": function(a) {
            	hideWarnings()
            	debugger
               if(! this._inhibitGotoTab) {
            	   
         	       var tabId = $(a.currentTarget).attr('href');
         	       var btn = $(a.currentTarget)
         	       this.gotoTab(tabId, btn)
         	   }
             },*/
            "click #enableFacebook" : function(a) {
            	var btn = $(a.currentTarget)
            	if(btn.hasClass("active")) {
            		this.model.set('fbAuth', null)
            	}
            	else {
            		var zis = this
            		this.checkFacebookStatus(function() {
                		FB.Event.subscribe('auth.statusChange', function(response) {
                			zis.checkFacebookStatus()
                		})
            			FB.login()
            		})
            	}
            },
            //always hide warnings on a tab switch
    	    "click [href^=#step]": function() {
    	       hideWarnings()
            },
            'keyup input[id=ownerEmail]' : function(e) {
            	this.model.set('ownerEmail', $(e.currentTarget).val())
            },
            'keyup input[id=ownerName]' : function(e) {
            	var t = $(e.currentTarget).val()
            	this.$('input[id=publicName]').val(t)
            	this.model.set('ownerName', t)
            },
            'keyup input[id=publicName]' : function(e) {
            	var t = $(e.currentTarget).val()
            	this.$('input[id=ownerName]').val(t)
            	this.model.set('ownerName', t)
            },
            'keyup input[name=title]' : function(e) {
                this.model.set('title', $(e.currentTarget).val())
            },
            "click #closeWizard": function() {
            	hideWarnings()
            },
            "click #confirm-public": function() {
            	this.postDecision()
            },            
            "click #confirm-private": function() {
            	this.postDecision()
            }            
        },
        postDecision: function() {
        	var zis = this
        	var d = this.model.toJSON()
        	var choices = this._choicesListView.model.toJSON()
        	d.choices = choices
        	debugger;
        	
            $.ajax({
                type: 'POST',
                url: "/dec",
                data: JSON.stringify(d),
                success: function(returnCode) {
            	  if(returnCode == "private-email")
            		  zis.pleaseReplyToEmailDialog()
                  else if(returnCode == "private-fb") {
                	  zis.close()
                	  new ApplicationView(zis.model.get('linkGuids').publicGuid)
                  }
                  else if(returnCode == "public") {
                	  zis.close()
                	  new ApplicationView(zis.model.get('linkGuids').adminGuid)
                  } 
                  else throw Error("Invalid return code " + returnCode)
                },
                error: function() {
                	$(zis.el).find('#fbConnect').text('Connect To FB')
                },
                contentType: "application/json; charset=utf-8",
                dataType: 'json'
            })
        },
        gotoTab: function(tabId, btn) {
           var zis = this;
 	       var v = this.validatorsBeforeTabs[tabId]
	       v && v(this, function() {
	    	 hideWarnings()
	    	 zis.enableTab(tabId)
	    	 //this.$(tabId).tab('show')
	    	 zis._inhibitGotoTab = true
	         btn.trigger('click')
	         zis._inhibitGotoTab = false
	       })        	
        },
    	model: new Decision(),
        initialize: function() {
        	var zis = this;
        	$.get('/guid', function(rawGuid) {
        		var linkGuids = JSON.parse(rawGuid)
        		zis.model.set('linkGuids', linkGuids);
        		zis.postInitialize();
        	})
        },
        pleaseReplyToEmailDialog: function() {
        	this.$('#createDecisionWizard .modal-body').html(Templates.pleaseReplyToEmailTemplate(this.model.toJSON()))
        },
    	postInitialize: function() {
        	this.render();
        	var zis = this;

         	zis.$('#enableFacebook').text(linkTofacebookButtonCaption())
        	zis.$('#enableFacebook').removeClass("active")

        	this.model.on('change', function() {

        		if(zis.model.hasChanged('title')) 
        			zis._titleOk(true)
        		
        		if(zis.model.hasChanged('ownerEmail')) 
        			zis._ownerEmailOk(true)
        		
        		if(zis.model.hasChanged('fbAuth')) FB.api('/me', function(meResp) {
                	if(zis.model.get('fbAuth')) {
                 	  zis.$('#enableFacebook').text(linkTofacebookButtonCaption(meResp.name))
                	  zis.$('#enableFacebook').addClass("active")
                	  zis._enableFacebookWarning.hide()
                	}
                	else {
                   	  zis.$('#enableFacebook').text(linkTofacebookButtonCaption())
                	  zis.$('#enableFacebook').removeClass("active")
                	}
                })

                if(zis.model.hasChanged('isPublic')) {
                	if(zis.model.get('isPublic')) {
                		zis.$("#registrationPublic").addClass("active")
                		zis.$("#registrationPrivate").removeClass("active")
                		zis.hideOrShowTab('#step3-public', true);
                		zis.hideOrShowTab('#step3-private', false);
                		zis.hideOrShowTab('#step4-private', false);
            	    }
            	    else {
                	    zis.$("#registrationPublic").removeClass("active")
                	    zis.$("#registrationPrivate").addClass("active")
                	    zis.hideOrShowTab('#step3-private', true);
                	    zis.hideOrShowTab('#step4-private', true);
                	    zis.hideOrShowTab('#step3-public', false)
                	}
                }
        	})

    	    this._titleWarning = createPopup(this.el,'', "Enter a meaningful title", TITLE_SELECTOR)
        	this._notEnoughChoicesWarning = createPopup(this.el,'', "Give voters at least two choices !", '#choiceList input:first-child')
        	this._duplicateChoiceWarning = createPopup(this.el,'', "This choice already exists", '#choiceList input:first-child')
        	this._ownerEmailWarning = createPopup(this.el,'', "The email address you entered is invalid", '#ownerEmail')
        	this._enableFacebookWarning = createPopup(this.el,'', "You need click on 'Link to Facebook'", '#enableFacebook')

    	    this._titleOk = function(noWarning) {
    	    	var t = zis.model.get('title')
    	    	var ok = t && t.length > 3
    	    	if(ok) zis._titleWarning.hide()
    	    	else {
    	    	  if(!noWarning) {
    	    		  zis.disableTab("#step2")
    	              zis.disableTab("#step3-public")
    	              zis.disableTab("#step3-private")
    	              zis.disableTab("#step4-private")
    	    		  zis._titleWarning.show()
    	    	  }
    	    	}
    	    	return ok
    	    }
    	    	
    	    this._choicesOk = function(noWarning) {
    	    	var ok = zis._choicesListView.model.length >= 2
    	    	if(ok) zis._notEnoughChoicesWarning.hide()
    	    	else {
    	    	  if(!noWarning) {
    	    		  zis._duplicateChoiceWarning.hide()
    	    		  zis._notEnoughChoicesWarning.show()
    	    	  }
    	    	}
    	    	return ok
    	    }
        			
    	    this._ownerEmailOk = function(noWarning) {
    	    	var t = zis.model.get('ownerEmail')
    	    	var ok = t && validateEmail(t) 
    	    	if(ok) zis._ownerEmailWarning.hide()
    	    	else {
    	    	  if(!noWarning) {
    	    		  zis.disableTab("#step4-private")
    	    		  zis._ownerEmailWarning.show()
    	    	  }
    	    	}
    	    	return ok
    	    }

    	    this._enableFacebookOk = function(noWarning) { 
    	    	var ok = zis.model.get('fbAuth') 
    	    	if(ok) zis._enableFacebookWarning.hide()
    	    	else {
    	    	  if(!noWarning) {
    	    		  zis.disableTab("#step4-private")
    	    		  zis._enableFacebookWarning.show()
    	    	  }
    	    	}
    	    	return ok
    	    }
    	    
    	    zis.disableTab("#step2")
    	    zis.disableTab("#step3-public")
    	    zis.disableTab("#step3-private")
    	    zis.disableTab("#step4-private")
    	    
    	    
        	this.modal = this.$('#createDecisionWizard').modal({backdrop: 'static'}).data('modal')

    		this._voteOnInvitationOnly = this.$('#voteOnInvitationOnly').collapse({toggle:false}).data('collapse')
    		this._anyOneCanVote = this.$('#anyOneCanVote').collapse().data('collapse')

        	this._choicesListView.duplicateHandler = function(isDuplicate, text) {
        		if(isDuplicate) {
        			zis._notEnoughChoicesWarning.hide()
        			zis._duplicateChoiceWarning.show()
        		}
        		else zis._duplicateChoiceWarning.hide()
        	}
        	
        	zis._choicesListView.model.on('add', function() {
        		zis._notEnoughChoicesWarning.hide()
        	},this)
        	
        	zis._choicesListView.model.on('remove', function() {
	    	    if(! zis._choicesOk(true)) {
	    		    zis.disableTab("#step2")
	                zis.disableTab("#step3-public")
	                zis.disableTab("#step3-private")
	                zis.disableTab("#step4-private")
	    	    }        		
        	},this)

        	this.model.set('isPublic', true)
        	initFacebook(this, false)
        	
        	this.modal.show()
    	},
    	render: function() {
    		var zis = this;
    		$(this.el).html($(Templates.createDecisionWizardTemplate(this.model.toJSON())))
    		this.$('#choiceList').html(this._choicesListView.render().el)
    		$('body').append(this.el)
    		return this
    	},
    	hideOrShowTab: function(tabId, setVisible) {
    		var tabLink = this.$('[href='+tabId + ']')
    		var tabBody = this.$(tabId)
    		if(setVisible) {
    			tabLink.css({display:''})
    			tabBody.css({display:''})
    		}
    		else {
    			tabLink.css({display:'none'})
    			tabBody.css({display:'none'})
    		}
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

    		this._linkToFacebook = this.$('#linkToFacebook').collapse({toggle:false}).data('collapse')
    		this._linkToEmail = this.$('#linkToEmail').collapse().data('collapse')
      	    this.$("a[href=#linkToFacebook]").addClass("active")
      	    this.$('#enableFacebook').text(linkTofacebookButtonCaption())
        },
        checkFacebookStatus: function(notLoggedInFunc) {
        	var zis = this;
        	FB.getLoginStatus(function(response) {
        		if(response.status === 'not_authorized') { 
        			zis.loggedInFacebook()
        			zis.model.set('fbAuth', null)
        	    }
        		else if (response.status === 'connected') {
        			//Connected to FB and App Authorized
        			zis.loggedInFacebook()
        			zis.model.set('fbAuth', response.authResponse)
        		}
        		else {
        			zis.loggedOutFacebook()
        			if(notLoggedInFunc) notLoggedInFunc()
        		}
            })
        },
        ready: function() {
        	this.checkFacebookStatus()
        },
        loggedOutFacebook: function() {
    		this._linkToFacebook = this.$('#linkToFacebook').collapse().data('collapse')
    		this._linkToEmail = this.$('#linkToEmail').collapse({toggle:false}).data('collapse')
    		this.$("a[href=#linkToFacebook]").removeClass("active")
    		this.$("a[href=#linkToEmail]").addClass("active")
    		this.model.set('fbAuth', null)
        }
    })
    
    var validateEmail = function (email) { 
      // http://stackoverflow.com/a/46181/11236
        var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(email);
    }    
    return new V()
}
