(function() {
  $(document).ready(function() {
    $.getJSON('../rest/mapmeter/data.json', function(data) {

      var stats = data;

      var eltSpec = '#mapmeter-chart';
      var jqueryElt = $(eltSpec);
      var d3Container = d3.select(eltSpec);

      // 1. set up the elements for the chart
      // 2. inject the data from the json response

      // hard coded width/height
      //var width = 500;
      //var height = 400;

      var width = jqueryElt.width();
      var height = jqueryElt.height();

      // set up all the elements needed for d3
      var xScale = d3.time.scale().range([0, width]),
          yScale = d3.scale.linear().range([height, 0]),
          xAxis = d3.svg.axis().scale(xScale).ticks(4),
          yAxis = d3.svg.axis().scale(yScale).ticks(3).orient('left');

      var yFormat = d3.format(',f');

      var area = d3.svg.area()
        .interpolate('monotone')
        .x(function(d) { return xScale(d.date); })
        .y0(height)
        .y1(function(d) { return yScale(d.value); });

      var line = d3.svg.line()
        .interpolate('monotone')
        .x(function(d) { return xScale(d.date); })
        .y(function(d) { return yScale(d.value); });

      var margin = {top: 5, right: 0, bottom: 20, left: 50};
      var svg = d3Container.append('svg')
        .attr('width', width + margin.right + margin.left)
        .attr('height', height + margin.top + margin.bottom);
      var g = svg.append('g')
               .attr('transform', 'translate(' +
          margin.left + ',' + margin.top + ')');

      var areaEl = g.append('path')
        .attr('class', 'area');

      var xAxisEl = g.append('g')
        .attr('class', 'x axis')
        .attr('transform', 'translate(0,' + height + ')');

      var yAxisEl = g.append('g')
        .attr('class', 'y axis');

      var pathEl = g.append('path')
        .attr('class', 'line');

      // populate the data
      var values = [],
          times = stats.data.time,
          len = times.length,
          startDate = new Date(1000 * times[0]),
          endDate = new Date(1000 * times[len - 1]);

      // extract arrays of metrics
      var metrics = {};
      $.each(stats.data, function(key, values) {
        // no $.isArray in this version of jquery
        //if ($.isArray(values)) {
        if (Object.prototype.toString.call(values) === '[object Array]') {
          metrics[key] = values;
        }
      });

      // build values per date by parsing the given expression
      var maxValue = 0,
          sum = 0,
          i, context, key, value;
      for (i = 0; i < len; ++i) {
        context = {};
        for (key in metrics) {
          context[key] = metrics[key][i];
        }
        // TODO figure out what is supposed to be happening here
        // value = $parse(attrs.expression)(scope, context) || 0;
        value = context.request_count;
        values[i] = {
          value: value,
          date: new Date(1000 * times[i])
        };
        maxValue = Math.max(value, maxValue);
        sum += value;
      }

      xScale.domain([startDate, endDate]);
      yScale.domain([0, maxValue]).nice();

      /* Redraw chart with updated values */
      areaEl.attr('d', area(values));
      xAxisEl.call(xAxis);
      yAxisEl.call(yAxis);
      pathEl.attr('d', line(values));
    });
  });
}());
