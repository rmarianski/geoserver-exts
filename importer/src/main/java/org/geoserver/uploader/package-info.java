/**
 * This package contains the classes needed to provide the resource upload through
 * HTTP POST method in multipart/form-data media type.
 * <p>
 * That is, in order to allow the upload of shapefiles, geotiffs, and in the future other 
 * types of spatial datasets through an HTML form.
 * </p>
 * <p>
 * <h2>REST endpoint</h2>
 * The {@code <geoserver context>/rest/upload} HTTP end point accepts multipart form data 
 * POST requests with the following form fields:
 * <ul>
 * <li>{@code file}: Mandatory. {@link FileItem} containing the uploaded file. I.e. the file
 * field in the HTML form shall called 'file'.
 * <li>{@code workspace}: Optional: workspace name where to import the spatial data file(s)
 * contained in {@code file}
 * <li>{@code store}: Optional: target store name (belonging to {@code workspace} if provided,
 * or to the uploader's default workspace otherwise). If not provided uploaded resources are
 * kept in its original format under the GeoServer's data directory.
 * <li>{@code title}: Optional: uploaded resource title
 * <li>{@code abstract}: Optional: uploaded resource abstract
 * </ul>
 * </p>
 * <p>
 * <h2>Configuration</h2>
 * Uploader's default workspace and default datastore may differ of GeoServer's defaults.
 * To accomplish that, uploader has its own configuration file underneath the GeoServer 
 * "data directory" (e.g. {@code /home/user/geoserver_data/uploader.xml}), with the following
 * format:
 * <pre>
 * <code>
 * &lt;UploaderConfig&gt;
 *   &lt;defaultWorkspace&gt;ws_name&lt;/defaultWorkspace&gt;
 *   &lt;defaultDataStore&gt;ds_name&lt;/defaultDataStore&gt;
 * &lt;/UploaderConfig&gt;
 * </code>
 * </pre>
 * Both are optional. If neither is provided then the GeoServer's defaults will be used.
 * If {@code defaultDataStore} is present, then {@code defaultWorkspace} shall be present too.
 * and it should point to a data store that's capable
 * of creating new feature types (PostGIS, Directory Data Store, may be Oracle and ArcSDE). Otherwise
 * it'll be ignored and a warning shown in the logs.
 * </p> 
 * <p>
 * Uploaded rasters will be put on the corresponding workspace (user provided, uploader's default,
 * GeoServer default. In that order of precedence) and the data store will be ignored, as it's not
 * possible to store rasters in data stores, nor in existing coverage stores.
 * </p>
 */
package org.geoserver.uploader;

import org.apache.commons.fileupload.FileItem;

