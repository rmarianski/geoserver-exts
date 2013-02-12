package org.geoserver.monitor.gwc;

import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;

import com.google.common.base.Optional;

public class GwcStatistician {

    public GwcStatistics getGwcStats(Optional<Operation> optionalOperation,
            Object owsResponseDispatchedResult) {
        if (!optionalOperation.isPresent()) {
            return emptyStats();
        }

        Operation operation = optionalOperation.get();
        GetMapRequest mapRequest = OwsUtils.parameter(operation.getParameters(),
                GetMapRequest.class);
        if (mapRequest == null) {
            return emptyStats();
        }

        boolean isTiled = mapRequest.isTiled();
        boolean isCacheHit = false;

        if (isTiled && owsResponseDispatchedResult instanceof WebMap) {
            WebMap map = (WebMap) owsResponseDispatchedResult;
            for (String[] h : map.getResponseHeaders()) {
                if ("geowebcache-cache-result".equalsIgnoreCase(h[0])
                        && "HIT".equalsIgnoreCase(h[1])) {
                    isCacheHit = true;
                    break;
                }
            }
        }

        return new GwcStatistics(isTiled, isCacheHit);
    }

    private GwcStatistics emptyStats() {
        return new GwcStatistics(false, false);
    }

}
