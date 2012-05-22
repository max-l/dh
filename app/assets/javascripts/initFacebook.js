

//fbAuthStatusListener : 
// ready()
// loggedInFacebook(meResp, response.authResponse)
// loggedOutFacebook()

function initFacebook(fbAuthStatusListener) {
    window.fbAsyncInit = function() {
      FB.init({
        appId      : '300426153342097',
        status     : true, // check login status
        channelUrl : '//'+window.location.hostname+'/channel', // Path to your Channel File
        cookie     : true, // enable cookies to allow the server to access the session
        xfbml      : true  // parse XFBML
      });

      fbAuthStatusListener.ready();

      FB.Event.subscribe('auth.statusChange', function(response) {

    	 if(response.authResponse) {
             FB.api('/me', function(meResp) {
            	 fbAuthStatusListener.loggedInFacebook(meResp, response.authResponse)
             })
         }
         else
           fbAuthStatusListener.loggedOutFacebook()
      })
    };

    // Load the SDK Asynchronously
    (function(d){
       var js, id = 'facebook-jssdk', ref = d.getElementsByTagName('script')[0];
       if (d.getElementById(id)) {return;}
       js = d.createElement('script'); js.id = id; js.async = true;
       js.src = "//connect.facebook.net/en_US/all.js";
       ref.parentNode.insertBefore(js, ref);
     }(document));
};