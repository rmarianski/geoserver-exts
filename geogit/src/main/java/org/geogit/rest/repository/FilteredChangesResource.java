/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.rest.repository;

import static org.geogit.rest.repository.GeogitResourceUtils.getGeogit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RepositoryFilter;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.remote.BinaryPackedChanges;
import org.geogit.remote.FilteredDiffIterator;
import org.geogit.repository.Repository;
import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

import com.google.common.base.Throwables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Gets a set of changes that match a provided filter from a particular commit.
 */
public class FilteredChangesResource extends Finder {

    @Override
    public Resource findTarget(Request request, Response response) {
        return new ChangesResource(getContext(), request, response);
    }

    private static class ChangesResource extends Resource {
        public ChangesResource(//
                Context context, //
                Request request, //
                Response response) //
        {
            super(context, request, response);
        }

        @Override
        public boolean allowPost() {
            return true;
        }

        @Override
        public void post(Representation entity) {
            try {
                final InputStream inStream;
                try {
                    inStream = entity.getStream();
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }

                final Reader body = new InputStreamReader(inStream);
                final JsonParser parser = new JsonParser();
                final JsonElement messageJson = parser.parse(body);

                final List<ObjectId> tracked = new ArrayList<ObjectId>();

                RepositoryFilter filter = new RepositoryFilter();

                ObjectId commitId = ObjectId.NULL;

                if (messageJson.isJsonObject()) {
                    final JsonObject message = messageJson.getAsJsonObject();
                    final JsonArray trackedArray;
                    if (message.has("tracked") && message.get("tracked").isJsonArray()) {
                        trackedArray = message.get("tracked").getAsJsonArray();
                    } else {
                        trackedArray = new JsonArray();
                    }
                    if (message.has("commitId") && message.get("commitId").isJsonPrimitive()) {
                        commitId = ObjectId.valueOf(message.get("commitId").getAsJsonPrimitive()
                                .getAsString());
                    } else {
                        commitId = ObjectId.NULL;
                    }
                    for (final JsonElement e : trackedArray) {
                        if (e.isJsonPrimitive()) {
                            tracked.add(ObjectId.valueOf(e.getAsJsonPrimitive().getAsString()));
                        }
                    }

                    if (message.has("filter") && message.get("filter").isJsonArray()) {
                        JsonArray filterArray = message.get("filter").getAsJsonArray();
                        for (final JsonElement e : filterArray) {
                            if (e.isJsonObject()) {
                                JsonObject filterObject = e.getAsJsonObject();
                                String featureType = null;
                                String filterType = null;
                                String filterText = null;
                                if (filterObject.has("featurepath")
                                        && filterObject.get("featurepath").isJsonPrimitive()) {
                                    featureType = filterObject.get("featurepath")
                                            .getAsJsonPrimitive().getAsString();
                                }
                                if (filterObject.has("type")
                                        && filterObject.get("type").isJsonPrimitive()) {
                                    filterType = filterObject.get("type").getAsJsonPrimitive()
                                            .getAsString();
                                }
                                if (filterObject.has("filter")
                                        && filterObject.get("filter").isJsonPrimitive()) {
                                    filterText = filterObject.get("filter").getAsJsonPrimitive()
                                            .getAsString();
                                }
                                if (featureType != null && filterType != null && filterText != null) {
                                    filter.addFilter(featureType, filterType, filterText);
                                }
                            }
                        }

                    }
                }

                final GeoGIT ggit = getGeogit(getRequest()).get();
                final Repository repository = ggit.getRepository();

                RevCommit commit = repository.getCommit(commitId);

                ObjectId parent = ObjectId.NULL;
                if (commit.getParentIds().size() > 0) {
                    parent = commit.getParentIds().get(0);
                }

                Iterator<DiffEntry> changes = ggit.command(DiffOp.class)
                        .setNewVersion(commit.getId()).setOldVersion(parent).setReportTrees(true)
                        .call();
                FilteredDiffIterator filteredChanges = new FilteredDiffIterator(changes,
                        repository, filter) {
                    @Override
                    protected boolean trackingObject(ObjectId objectId) {
                        return tracked.contains(objectId);
                    }
                };

                getResponse().setEntity(
                        new FilteredDiffIteratorRepresentation(new BinaryPackedChanges(repository),
                                filteredChanges));

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static final MediaType PACKED_OBJECTS = new MediaType("application/x-geogit-packed");

        private class FilteredDiffIteratorRepresentation extends OutputRepresentation {

            private final BinaryPackedChanges packer;

            private final FilteredDiffIterator changes;

            public FilteredDiffIteratorRepresentation(BinaryPackedChanges packer,
                    FilteredDiffIterator changes) {
                super(PACKED_OBJECTS);
                this.changes = changes;
                this.packer = packer;
            }

            @Override
            public void write(OutputStream out) throws IOException {
                packer.write(out, changes);
                // signal the end of changes
                out.write(2);
                if (changes.wasFiltered()) {
                    out.write(1);
                } else {
                    out.write(0);
                }
            }
        }
    }
}
