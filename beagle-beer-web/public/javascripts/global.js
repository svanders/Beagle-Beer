

$(document).ready( function() {

  /**
   * Bind a function to the 'x' link on all alerts on the page, so hide the alert div.
   */
  $('div.alert a').click(function(event) {
    event.preventDefault();
    $(this).parent().fadeOut();
  });

});


