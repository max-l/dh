
  $.fn.serializeObject = function() {
    var o = {};
    var a = this.serializeArray();
    $.each(a, function() {
      if (o[this.name] !== undefined) {
        if (!o[this.name].push) {
            o[this.name] = [o[this.name]];
        }
        o[this.name].push(this.value || '');
      } else {
        o[this.name] = this.value || '';
      }
    });
    return o;
  }
  
  
  function submitNewDecision() {

    var d = $('#endDate').datepicker('getDate').getTime()
    var formFields = $("#createOrEditDecisionForm").serializeObject()
    formFields.endDate = d

    formFields.choicesToDelete = persistedChoicesToDelete

    var choiceForms = 
      $('#alternatives form').map(function() {
        return $(this).serializeObject()
      }).get()

    formFields.alternativePosts = choiceForms

    var z = JSON.stringify(formFields)
     
    var ffs = $("#createOrEditDecisionForm .control-group")
     
    $.ajax({
      type: 'POST',
      url: "/decision/create",
      data: z,
      success: function(idOfNewDecision) {
        ajaxAction('/p/' + idOfNewDecision)
      },
      error: function(p1) {
        var errorMap = $.parseJSON(p1.responseText)
        ffs.map(function() {
          var inp = $(this).find('input')
          var fieldName = inp.attr('name')
          var errorMsg = errorMap[fieldName]
          if(errorMsg) {
            $(this).find('.help-inline').text(errorMsg)
            $(this).removeClass('success')
            $(this).addClass('error')
          } else {
            $(this).find('.help-inline').text('')
            $(this).removeClass('error')
            $(this).addClass('success')
          }
        })
      },
      contentType: "application/json; charset=utf-8",
      dataType: 'json'
    })
  }

  
  function inviteFrieldsFunc(msg, data) {
    return function(decisionId) {
      FB.ui({method: 'apprequests',
        message: msg,
        data: data
      },
      function requestCallback(response) {
        if(response != null) {
          response.decisionId = decisionId
          augmentAndPostInfo(response)
         }
      })
    }
  }

  function augmentAndPostInfo(response) {

    var fbIds = JSON.stringify(response.to).replace('[','(').replace(']',')')

    FB.api({
      method: 'fql.query',
        query: 'SELECT uid,name FROM user WHERE uid in ' + fbIds
      },
      function(queryResponse) {
        response.to = queryResponse
        var msgToPost = JSON.stringify(response)
        $.ajax({
          type: 'POST',
          url: "/recordInvitationList",
          data: msgToPost,
          success: function() {
            refreshCenterPanel()
          },
          contentType: "application/json; charset=utf-8",
          dataType: 'json'
        })
      }
    )
  }

