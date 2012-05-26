
    Decision = Backbone.Model.extend({
        defaults: function() {
            return {title: ""}
        },
        choiceList: _.once(function() {
            var ChoiceList = Backbone.Collection.extend({
               model: Backbone.Model,
               url: "/dec/alternatives/" + this.id
            });
            return new ChoiceList({id: this.id})
        }),
        urlRoot: "/dec",
        getFBParticipants: _.once(function() {
            FBParticipant = Backbone.Model.extend({});

            FBParticipantsList = Backbone.Collection.extend({
                model: FBParticipant,
                url: "/dec/participants/" + this.id
            });

        	return new FBParticipantsList()
        })
    });