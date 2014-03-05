/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */

package org.geogit.rest.repository;

import static org.geogit.rest.repository.GeogitResourceUtils.getGeogit;

import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.GeoGIT;
import org.geogit.remote.BinaryPackedObjects;
import org.geoserver.rest.RestletException;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

import com.google.common.io.Closeables;

/**
 *
 */
public class SendObjectResource extends Resource {

    @Override
    public boolean allowPost() {
        return true;
    }

    public void post(Representation entity) {
        InputStream input = null;

        try {
            input = getRequest().getEntity().getStream();
            final GeoGIT ggit = getGeogit(getRequest()).get();
            final BinaryPackedObjects unpacker = new BinaryPackedObjects(ggit.getRepository()
                    .getObjectDatabase());
            unpacker.ingest(input);

        } catch (IOException e) {
            throw new RestletException(e.getMessage(), Status.SERVER_ERROR_INTERNAL, e);
        } finally {
            if (input != null)
                Closeables.closeQuietly(input);
        }
    }
}
