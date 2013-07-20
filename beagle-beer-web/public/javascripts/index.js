$(document).ready(function () {


  /**
   * Bind a function to the on/off element links that performs an ajax call to
   *
   */
  $('#startLogger').click(function () {
    $.ajax(startLoggerUrl)
    EventBus.publish("loggerRunning")

  });
  $('#stopLogger').click(function () {
    $.ajax(stopLoggerUrl)
    EventBus.publish("loggerStopped")
  });

  EventBus.subscribe("loggerRunning", function () {
    if (INDEX_PAGE.loggingPollId < 0) {
      pollForLatestValue() // immediate poll
      INDEX_PAGE.loggingPollId = setInterval(pollForLatestValue, 5000)
    }
  });

  EventBus.subscribe("loggerStopped", function () {
    clearInterval(INDEX_PAGE.loggingPollId)
    INDEX_PAGE.loggingPollId = -1;
    resetGauges();
  });

  /**
   * Create the temperature gauges and start polling
   */
  createGauges();
  INDEX_PAGE.runningPollId = setInterval(pollForLoggerRunning, 500);

});


/** Page container to provide global abatement */
var INDEX_PAGE = {}

/** An object with named gauges, one for each ds1820 probe. */
INDEX_PAGE.gauges = {};

/** Variable to track the stat of if the logger is running */
INDEX_PAGE.loggingPollId = -1;


function pollForLoggerRunning() {
  $.ajax({
    type: 'GET',
    dataType: 'json',
    url: isRunningUrl,
    success: (function (isRunning) {
      if (!isRunning) {
        EventBus.publish("loggerStopped")
      } else {
        EventBus.publish("loggerRunning")
      }
    })
  });
}


function pollForLatestValue() {
  $.ajax({
    type: 'GET',
    dataType: 'json',
    url: latestValueUrl,
    success: updateGauges
  });
}


function createGauge(name, label, min, max) {
  var config = {
    size: 200,
    label: label,
    min: undefined != min ? min : 0,
    max: undefined != max ? max : 100,
    minorTicks: 5
  }

  var range = config.max - config.min;
  config.yellowZones = [
    { from: config.min + range * 0.75, to: config.min + range * 0.9 }
  ];
  config.redZones = [
    { from: config.min + range * 0.9, to: config.max }
  ];

  INDEX_PAGE.gauges[name] = new Gauge(name, config);
  INDEX_PAGE.gauges[name].render();
}

function createGauges() {
  $("#gauges > span").each(function () {
    createGauge($(this).attr("id"), $(this).attr("title"), 0, 30);
  });
}

function updateGauges(samples) {
  if (samples.length === 0) {
    resetGauges()
  } else {
    samples.map(function (sample) {
      INDEX_PAGE.gauges["gauge" + sample.ds1820Id].redraw(sample.value)
    })
  }
}

function resetGauges() {
  for (var key in INDEX_PAGE.gauges) {
    INDEX_PAGE.gauges[key].redraw(0);
  }
}

