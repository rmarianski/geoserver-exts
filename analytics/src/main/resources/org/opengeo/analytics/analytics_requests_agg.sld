<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>Requests</Name>
    <UserStyle>
      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Stroke>
                  <CssParameter name="stroke">#EE9900</CssParameter>
                  <CssParameter name="stroke-width">1.5</CssParameter>
                </Stroke>
                <Fill>
                  <CssParameter name="fill">#EE9900</CssParameter>
                  <CssParameter name="fill-opacity">0.75</CssParameter>
                </Fill>
              </Mark>
              <Size>
                <ogc:Add>
                  <ogc:Function name="max">
                    <ogc:Function name="log">
                      <ogc:PropertyName>REQUESTS</ogc:PropertyName>
                    </ogc:Function>
                    <ogc:Literal>1</ogc:Literal>
                  </ogc:Function>
                  <ogc:Literal>5</ogc:Literal>
                </ogc:Add>
              </Size>
            </Graphic>
          </PointSymbolizer>
          <TextSymbolizer>
             <Label>
               <ogc:PropertyName>REMOTE_CITY</ogc:PropertyName>
             </Label>
             <Font>
               <CssParameter name="font-family">Arial</CssParameter>
               <CssParameter name="font-size">12</CssParameter>
               <CssParameter name="font-style">normal</CssParameter>
               <CssParameter name="font-weight">bold</CssParameter>
             </Font>
             <LabelPlacement>
               <PointPlacement>
                 <AnchorPoint>
                   <AnchorPointX>0.5</AnchorPointX>
                   <AnchorPointY>0.0</AnchorPointY>
                 </AnchorPoint>
                 <Displacement>
                   <DisplacementX>0</DisplacementX>
                   <DisplacementY>5</DisplacementY>
                 </Displacement>
               </PointPlacement>
             </LabelPlacement>
             <Fill>
               <CssParameter name="fill">#000000</CssParameter>
             </Fill>
          </TextSymbolizer>
        </Rule>

      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
