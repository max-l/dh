

console.log("start....");

Example = Ember.Application.create({ 
 name: "Example Application",
 logo: "http://sympodial.com/images/logo.png",
 searchString: "%23EmberJS",
 ready: function() {
  //Example.populate.getTweets();
  //setInterval(function() {Example.populate.getTweets();}, 2000);
 } //.observes("name")
})

Example.Item = Ember.Object.extend();
Example.LoopingView = Ember.View.extend();

Example.ToSearchValue = Ember.Object.create({
    value: '#Zaza'
});

Example.ToSearch = Ember.TextField.extend({
    valueBinding: 'Example.ToSearchValue.value',
    keyUp: function(e) {
        this.interpretKeyEvents(event);
        if (e.keyCode == 13) {
            console.log(Example.ToSearchValue.get('value'))
            Example.populate.clear();
            Example.populate.getTweets()
        }
    }
});

Example.populate = Ember.ArrayController.create({ 
 content: [],
 idArray: {},
 addItem: function(item) { 
  var id = item.id;
  if(typeof this.idArray[id]  == "undefined") { 
   if(item.iso_language_code == "en") { 
    this.pushObject(item);
    this.idArray[id] = item.id;
    Example.Item.create({ name: item.text });
   }
  };
 },
 getTweets: function() { 

  var self = this;
  var searchString = Example.get("searchString");

  searchString = Example.ToSearchValue.get('value')
  searchString = searchString.replace('#','%23')
  console.log('->' + searchString);
  var url = "http://search.twitter.com/search.json?callback=?&q=" + searchString;
  $.getJSON(url, function(data) {
   console.log("fetch : " + url)
   console.log(data)
   if(data && data.results) { 
     for (var i = 0; i < data.results.length; i++) { 
      self.addItem(Example.Item.create(data.results[i]));
     };
   }
  })
 }.observes("Example.searchString")
});
