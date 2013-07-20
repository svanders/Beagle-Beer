$(document).ready(function () {

  /**
   * Bind a function to the 'x' link on all alerts on the page, so hide the alert div.
   */
  $('div.alert a').click(function (event) {
    event.preventDefault();
    $(this).parent().fadeOut();
  });

});

/**
 * An event bus/manager to provide basic event management.
 */
var EventBus = {
  subscribe: function (event, fn) {
    $(this).bind(event, fn);
  },
  unsubscribe: function (event, fn) {
    $(this).unbind(event, fn);
  },
  publish: function (event) {
    $(this).trigger(event);
  }
};



