(function() {
  $(document).ready(function() {
    var chartElt = $('<div></div>').attr('id', 'mapmeter-chart');
    $('.page-pane').append(chartElt);
    var chartDomElt = chartElt.get(0);
    mapmeter.fetchDataAndDrawChart(chartDomElt);
  });
}());

