!function($) {

    /* DATETIMEPICKER DATA-API
     * ================== */

     var _extractTimeArray = function(time) {

         var tp = new $.fn.timepicker.Constructor();
         tp.setValues(time);

         return [tp.meridian, tp.hour, tp.minute] 
     };

     var _extractDateTime = function(date, time) {

    	 var ms = date.getTime();
    	 var dt = new Date(ms);
         var dta = _extractTimeArray(time);
         dt.setHours(dta[1]);
         dt.setMinutes(dta[2]);
         return dt;
     };

     var _truncateDate = function(date) {
      	date.setHours(0);
      	date.setMinutes(0);
      	date.setSeconds(0);
      	date.setMilliseconds(0);
      	return date;
     };

     $.fn.datetimepicker = function (option) {
        return this.each(function () {
            var $this = $(this);
            var data = $this.data('datetimepicker');

            var options = (typeof option == 'object') && option;
            if (!data) {
                $this.data('datetimepicker', (data = new Datetimepicker(this, options)));
            }
            if ((typeof option) == 'string') {
                data[option]();
            }
        })
     };

     var Datetimepicker = function(element, options) {
         this.$element = $(element);
         this.options = $.extend({}, $.fn.datetimepicker.defaults, options);
         this.init(this.options);
     };

     Datetimepicker.prototype = {
        constructor: Datetimepicker,
        init: function(options) {

    	    var e = this.$element;
    	    var datePickerOptions = {
    	        minDate: options.minDate
    	    };
    	    var zis = this;

    	    if(options.onChange)
    	       datePickerOptions.onSelect = function() {
                   var dt = zis.getDatetime()
                   if(dt) options.onChange(dt)
   			   };

     		var w1 = $('<input type="text">').datepicker(datePickerOptions);
     		
     		var w2 = $('<input type="text">').timepicker({showMeridian: false});

    	    if(options.onChange) {
    	      w2.blur(function(e) {
                  var dt = zis.getDatetime()
                  if(dt) options.onChange(dt)
   			  });
    	    }

    	    $(this).data('datetimepicker', w1);
    	    $(this).data('timepicker', w2);

    	    $(w1).css("width","95px");
    	    $(w2).css("width","95px");

    		$(e).append(w1).append("&nbsp;&nbsp;");	
    		$(e).append(w2);

    		var defaultDate = this.options.defaultDatetime || new Date();

    		this.setDatetime(defaultDate)
        },
        getDatepicker: function () {
        	return $(this).data('datetimepicker')
        },
        getTimepicker: function () {
        	return $(this).data('timepicker')
        },        
        getDatetime: function() {
        	var w1 = this.getDatepicker();
        	var dt = w1.datepicker("getDate")

        	if(w1 == null) return null;
        	var w2 = this.getTimepicker();
        	
        	if($(w2)[0].value == "") 
        	  return _truncateDate(w1.datepicker("getDate"));

        	var res = _extractDateTime(
        		dt,
        		$(w2)[0].value)
        		
            res.setSeconds(0);
            res.setMilliseconds(0);
            return res
        },
        setDatetime: function(datetime) {

        	var w1 = this.getDatepicker();
        	var w2 = this.getTimepicker();
        	w1.datepicker("setDate", datetime);
        	var d = new Date(datetime.milliseconds);

        	var h = datetime.getHours();
        	var m = datetime.getMinutes();
        	m = Math.round(m/5) * 5;
    		$(w2)[0].value = h + ":" + m
    		var tp = w2.data('timepicker');
    		tp.updateFromElementVal();
        }
    };

     $(function () {
        $('body').ready(function() {
        	$('div[data-provide="datetimepicker"]').each(function(i,e) {

                var $this = $(this);
                if ($this.data('datetimepicker')) {
                    return;
                }
                $this.datetimepicker($this.data());
        	})
        })
     })
}(window.jQuery);
