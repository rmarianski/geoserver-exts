/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */

package org.geogit.rest.repository;

import java.util.List;
import java.util.Map;

import org.geoserver.rest.MapResource;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.FreemarkerFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Access point to a single repository.
 * <p>
 * Defines the following repository ends points:
 * <ul>
 * <li>{@code /manifest}
 * </ul>
 */
public class RepositoryResource extends MapResource {

    @Override
    public Map<String, Object> getMap() throws Exception {

        Map<String, Object> map = Maps.newHashMap();
        PageInfo pageInfo = getPageInfo();
        map.put("page", pageInfo);

        map.put("Manifest", "manifest");
        return map;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = Lists.newArrayListWithCapacity(3);

        formats.add(new FreemarkerFormat(RepositoryResource.class.getSimpleName() + ".ftl",
                getClass(), MediaType.TEXT_HTML));

        return formats;
    }

}
