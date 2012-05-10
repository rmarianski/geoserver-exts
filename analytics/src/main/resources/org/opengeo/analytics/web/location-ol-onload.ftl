    map = new OpenLayers.Map({
        div: "${markupId}",
        projection: new OpenLayers.Projection("EPSG:900913"),
        displayProjection: new OpenLayers.Projection("EPSG:4326"),
        units: "m",
        numZoomLevels: 18,
        maxResolution: 156543.0339,
        maxExtent: new OpenLayers.Bounds(-20037508, -20037508, 20037508, 20037508.34),
        controls: [
            new OpenLayers.Control.Navigation({zoomWheelEnabled:false}),
            new OpenLayers.Control.PanZoomBar(),
            new OpenLayers.Control.KeyboardDefaults()
        ]
    });

        
    var osm = new OpenLayers.Layer.OSM();
    var wms = new OpenLayers.Layer.WMS(
        "Requests", "../wms",
        {'layers': 'analytics:requests_agg', 'format':'image/png', 'transparent':true, 'viewparams':"query:${query}"},
        {
            'isBaseLayer': false,
            'singleTile': true
        }
    );
    
    map.addLayers([osm, wms]);
    map.zoomToMaxExtent();
    