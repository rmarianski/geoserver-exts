(function() {
  $(document).ready(function() {
    var chartDomElt = $('#mapmeter-chart').get(0);
    mapmeter.fetchDataAndDrawChart(chartDomElt);
  });
}());
