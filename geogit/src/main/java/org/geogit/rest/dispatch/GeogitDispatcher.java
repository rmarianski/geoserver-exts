/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */

package org.geogit.rest.dispatch;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geogit.rest.repository.CatalogRepositoryProvider;
import org.geogit.rest.repository.CommandResource;
import org.geogit.rest.repository.RepositoryListResource;
import org.geogit.rest.repository.RepositoryProvider;
import org.geogit.rest.repository.RepositoryResource;
import org.geogit.rest.repository.RepositoryRouter;
import org.geogit.rest.repository.RestletException;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.GeoServerServletConverter;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RESTDispatcher;
import org.geoserver.rest.RESTMapping;
import org.geotools.util.logging.Logging;
import org.restlet.Router;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.springframework.beans.BeansException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.noelios.restlet.ext.servlet.ServletConverter;

/**
 * Simple AbstractController implementation that does the translation between Spring requests and
 * Restlet requests.
 * <p>
 * Almost a verbatim copy of {@link RESTDispatcher} but looks for {@link GeogitRESTMapping} instead
 * of {@link RESTMapping} so that our mappings don't get added to the regular geoserver rest
 * dispatcher.
 */
public class GeogitDispatcher extends AbstractController {
    /** HTTP method "PUT" */
    public static final String METHOD_PUT = "PUT";

    /** HTTP method "DELETE" */
    public static final String METHOD_DELETE = "DELETE";

    /**
     * logger
     */
    static Logger LOG = Logging.getLogger(GeogitDispatcher.class);

    private CatalogRepositoryProvider repositoryProvider;

    /**
     * converter for turning servlet requests into resetlet requests.
     */
    private ServletConverter converter;

    /**
     * the root restlet router
     */
    private Router router;

    public GeogitDispatcher(final Catalog catalog) {
        this.repositoryProvider = new CatalogRepositoryProvider(catalog);
        setSupportedMethods(new String[] { METHOD_GET, METHOD_POST, METHOD_PUT, METHOD_DELETE,
                METHOD_HEAD });
    }

    @Override
    protected void initApplicationContext() throws BeansException {
        super.initApplicationContext();

        converter = new GeoServerServletConverter(getServletContext());
        router = createInboundRoot();
        converter.setTarget(router);
    }

    public Router createInboundRoot() {
        Router router = createRoot();
        router.attach("", RepositoryListResource.class);
        router.attach("/", RepositoryListResource.class);
        router.attach("/{repository}", RepositoryResource.class);
        router.attach("/{repository}/repo", makeRepoRouter());
        router.attach("/{repository}/{command}.{extension}", CommandResource.class);
        router.attach("/{repository}/{command}", CommandResource.class);
        return router;
    }

    public static Router makeRepoRouter() {
        return new RepositoryRouter();
    }

    @Override
    public ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        try {
            converter.service(req, resp);
        } catch (Exception e) {
            RestletException re = null;
            if (e instanceof RestletException) {
                re = (RestletException) e;
            }
            if (re == null && e.getCause() instanceof RestletException) {
                re = (RestletException) e.getCause();
            }

            if (re != null) {
                resp.setStatus(re.getStatus().getCode());

                String reStr = re.getRepresentation().getText();
                if (reStr != null) {
                    LOG.severe(reStr);
                    resp.setContentType("text/plain");
                    resp.getOutputStream().write(reStr.getBytes());
                }

                // log the full exception at a higher level
                LOG.log(Level.SEVERE, "", re);
            } else {
                LOG.log(Level.SEVERE, "", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                if (e.getMessage() != null) {
                    resp.getOutputStream().write(e.getMessage().getBytes());
                }
            }
            resp.getOutputStream().flush();
        }

        return null;
    }

    public Router createRoot() {
        Router router = new Router() {

            @Override
            protected synchronized void init(Request request, Response response) {
                super.init(request, response);
                if (!isStarted()) {
                    return;
                }
                request.getAttributes().put(RepositoryProvider.KEY, repositoryProvider);
                // set the page uri's

                // http://host:port/appName
                String baseURL = request.getRootRef().getParentRef().toString();
                String rootPath = request.getRootRef().toString().substring(baseURL.length());
                String pagePath = request.getResourceRef().toString().substring(baseURL.length());
                String basePath = null;
                if (request.getResourceRef().getBaseRef() != null) {
                    basePath = request.getResourceRef().getBaseRef().toString()
                            .substring(baseURL.length());
                }

                // strip off the extension
                String extension = ResponseUtils.getExtension(pagePath);
                if (extension != null) {
                    pagePath = pagePath.substring(0, pagePath.length() - extension.length() - 1);
                }

                // trim leading slash
                if (pagePath.endsWith("/")) {
                    pagePath = pagePath.substring(0, pagePath.length() - 1);
                }
                // create a page info object and put it into a request attribute
                PageInfo pageInfo = new PageInfo();
                pageInfo.setBaseURL(baseURL);
                pageInfo.setRootPath(rootPath);
                pageInfo.setBasePath(basePath);
                pageInfo.setPagePath(pagePath);
                pageInfo.setExtension(extension);
                request.getAttributes().put(PageInfo.KEY, pageInfo);
            }

        };
        return router;
    }
}
