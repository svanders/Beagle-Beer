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
      $("#startLogger").hide();
      $("#stopLogger").show();
  });

  EventBus.subscribe("loggerStopped", function () {
    $("#stopLogger").hide()
    $("#startLogger").show()
  });

  /**
   * Create the temperature gauges and start polling
   */
  createGauges();
  // do an immediate poll
  pollForLoggerRunning();
  // now start regular polling
  INDEX_PAGE.runningPollId = setInterval(pollForLoggerRunning, 1000);

  pollForLatestValue() // immediate poll
  INDEX_PAGE.loggingPollId = setInterval(pollForLatestValue, 5000);

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

//var margin = {top: 20, right: 80, bottom: 30, left: 50},
//    width = 960 - margin.left - margin.right,
//    height = 500 - margin.top - margin.bottom;
//
//var parseDate = d3.time.format("%Y%m%d").parse;
//
//var x = d3.time.scale()
//    .range([0, width]);
//
//var y = d3.scale.linear()
//    .range([height, 0]);
//
//var color = d3.scale.category10();
//
//var xAxis = d3.svg.axis()
//    .scale(x)
//    .orient("bottom");
//
//var yAxis = d3.svg.axis()
//    .scale(y)
//    .orient("left");
//
//var line = d3.svg.line()
//    .interpolate("basis")
//    .x(function(d) { return x(d.date); })
//    .y(function(d) { return y(d.temperature); });
//
//var svg = d3.select("body").append("svg")
//    .attr("width", width + margin.left + margin.right)
//    .attr("height", height + margin.top + margin.bottom)
//    .append("g")
//    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
//
//d3.tsv("data.tsv", function(error, data) {
//  color.domain(d3.keys(data[0]).filter(function(key) { return key !== "date"; }));
//
//  data.forEach(function(d) {
//    d.date = parseDate(d.date);
//  });
//
//  var cities = color.domain().map(function(name) {
//    return {
//      name: name,
//      values: data.map(function(d) {
//        return {date: d.date, temperature: +d[name]};
//      })
//    };
//  });
//
//  x.domain(d3.extent(data, function(d) { return d.date; }));
//
//  y.domain([
//    d3.min(cities, function(c) { return d3.min(c.values, function(v) { return v.temperature; }); }),
//    d3.max(cities, function(c) { return d3.max(c.values, function(v) { return v.temperature; }); })
//  ]);
//
//  svg.append("g")
//      .attr("class", "x axis")
//      .attr("transform", "translate(0," + height + ")")
//      .call(xAxis);
//
//  svg.append("g")
//      .attr("class", "y axis")
//      .call(yAxis)
//      .append("text")
//      .attr("transform", "rotate(-90)")
//      .attr("y", 6)
//      .attr("dy", ".71em")
//      .style("text-anchor", "end")
//      .text("Temperature (ºF)");
//
//  var city = svg.selectAll(".city")
//      .data(cities)
//      .enter().append("g")
//      .attr("class", "city");
//
//  city.append("path")
//      .attr("class", "line")
//      .attr("d", function(d) { return line(d.values); })
//      .style("stroke", function(d) { return color(d.name); });
//
//  city.append("text")
//      .datum(function(d) { return {name: d.name, value: d.values[d.values.length - 1]}; })
//      .attr("transform", function(d) { return "translate(" + x(d.value.date) + "," + y(d.value.temperature) + ")"; })
//      .attr("x", 3)
//      .attr("dy", ".35em")
//      .text(function(d) { return d.name; });
//}

//);

