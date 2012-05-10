    map = new OpenLayers.Map({
        div: "${markupId}",
        projection: new OpenLayers.Projection("EPSG:900913"),
        displayProjection: new OpenLayers.Projection("EPSG:4326"),
        units: "m",
        numZoomLevels: 18,
        maxResolution: 156543.0339,
        maxExtent: new OpenLayers.Bounds(-20037508, -20037508, 20037508, 20037508.34)
    });

    var osm = new OpenLayers.Layer.OSM();
    //var gmap = new OpenLayers.Layer.Google("Google Streets", {visibility: false});

    var wkt = new OpenLayers.Format.WKT({
      externalProjection: new OpenLayers.Projection("EPSG:4326"),
      internalProjection: new OpenLayers.Projection("EPSG:900913")
    });
    
    vectors = new OpenLayers.Layer.Vector("Vectors");
    
    var feature = wkt.read('${origin}');
    vectors.addFeatures([feature]);
    
    map.addLayers([osm, vectors]);
    map.addControl(new OpenLayers.Control.LayerSwitcher());
    map.setCenter(new OpenLayers.LonLat(feature.geometry.x, feature.geometry.y));
    map.zoomTo(8);
