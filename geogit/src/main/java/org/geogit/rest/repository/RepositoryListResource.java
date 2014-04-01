/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.rest.repository;

import static org.geogit.rest.repository.RESTUtils.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.rest.MapResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.FreemarkerFormat;
import org.geoserver.rest.format.MapJSONFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RepositoryListResource extends MapResource {

    public RepositoryListResource() {
        super();
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = Lists.newArrayListWithCapacity(3);

        formats.add(new FreemarkerFormat(RepositoryListResource.class.getSimpleName() + ".ftl",
                getClass(), MediaType.TEXT_HTML));

        formats.add(new MapJSONFormat());

        return formats;
    }

    @Override
    public Map<String, Object> getMap() throws Exception {
        List<String> repoNames = getRepoNames();

        Map<String, Object> map = Maps.newHashMap();
        map.put("repositories", repoNames);
        map.put("page", getPageInfo());
        return map;
    }

    private List<String> getRepoNames() {
        Request request = getRequest();
        CatalogRepositoryProvider repoFinder = (CatalogRepositoryProvider) repositoryProvider(request);

        List<DataStoreInfo> geogitStores = repoFinder.findGeogitStores(request);

        List<String> repoNames = Lists.newArrayListWithCapacity(geogitStores.size());
        for (DataStoreInfo info : geogitStores) {
            String wsname = info.getWorkspace().getName();
            String storename = info.getName();
            String repoName = wsname + ":" + storename;
            repoNames.add(repoName);
        }
        Collections.sort(repoNames);
        return repoNames;
    }
}
