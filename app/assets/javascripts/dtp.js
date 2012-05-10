

MM = Backbone.Model.extend({
  defaults: function() {
    return {
      choice:1,
      dynValue:true
    }
  }
});

mm = new MM();

var  _templateForChoice1 = _.template($('#checkboxW').html());
var  _templateForChoice2 = _.template($('#textfieldW').html());

VV = Backbone.View.extend({    
  el: $("#dtp"),
  _templateRadio: _.template($('#mainTemplate').html()),
  initialize: function() {
     $(this.el).html(this._templateRadio());
     this.render()
  },
  render: function() {
    var ch = mm.get('choice');

    var templ = function() {      
      if(ch == 1) return _templateForChoice1;
      if(ch == 2) return _templateForChoice2;
      throw ('unknown choice ' + ch)
    };
    var dp = this.$("#dynPart");
    var d = templ()({dynValue: ch});
    dp.html(d)
  },
  events: {
    "click input[name=choice]": "_selectWidget"
  },
  _selectWidget: function(e) {
    console.log('zaza');
    var v = e.currentTarget.value;
    //console.log(mm);
    mm.set('choice', v);
    this.render()
  }
});

new VV();
