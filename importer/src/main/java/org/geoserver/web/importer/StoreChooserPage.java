/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataStoreFactorySpi;

/**
 * First page in the importer, the store chooser one that will redirect to the proper parameter
 * collecting page
 * 
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class StoreChooserPage extends GeoServerBasePage {

    public StoreChooserPage(PageParameters params) {
        if ("TRUE".equalsIgnoreCase(params.getString("afterCleanup")))
            info(new ParamResourceModel("rollbackSuccessful", this).getString());

        ListView storeLinks = new ListView("stores", Store.getAvailableStores()) {

            @Override
            protected void populateItem(ListItem item) {
                Store store = (Store) item.getModelObject();
                BookmarkablePageLink link = new BookmarkablePageLink("storeLink", store
                        .getDestinationPage());
                link.add(new Label("storeName", store.getStoreName(StoreChooserPage.this)));
                item.add(link);
                item.add(new Label("storeDescription", store
                        .getStoreDescription(StoreChooserPage.this)));
                Image icon = new Image("storeIcon", store.getStoreIcon());
                icon.add(new AttributeModifier("alt", true, store
                        .getStoreDescription(StoreChooserPage.this)));
                item.add(icon);
            }
        };
        add(storeLinks);
    }

    /**
     * A store we know about. It is usually a 1-many match towards GeoTools data stores (one of
     * these matches more than one data store factory in particular)
     */
    enum Store {
        directory(new ResourceReference(GeoServerApplication.class, "img/icons/silk/folder.png"),
                DirectoryPage.class, "org.geotools.data.shapefile.ShapefileDataStoreFactory"), // 
        postgis(new ResourceReference(GeoServerApplication.class,
                "img/icons/geosilk/database_vector.png"), PostGISPage.class,
                "org.geotools.data.postgis.PostgisNGDataStoreFactory"), //
        oracle(new ResourceReference(GeoServerApplication.class,
                "img/icons/geosilk/database_vector.png"), OraclePage.class,
                "org.geotools.data.oracle.OracleNGDataStoreFactory"), //
        sqlserver(new ResourceReference(GeoServerApplication.class,
                "img/icons/geosilk/database_vector.png"), SQLServerPage.class,
                "org.geotools.data.sqlserver.SQLServerDataStoreFactory"), //
        arcsde(new ResourceReference(GeoServerApplication.class,
                "img/icons/geosilk/database_vector.png"), ArcSDEPage.class,
                "org.geotools.arcsde.ArcSDEDataStoreFactory");

        ResourceReference icon;

        Class<? extends Page> destinationPage;

        String factoryClassName;

        Store(ResourceReference icon, Class<? extends Page> destinationPage, String factoryClassName) {
            this.icon = icon;
            this.destinationPage = destinationPage;
            this.factoryClassName = factoryClassName;
        }

        IModel getStoreName(Component component) {
            return new ParamResourceModel(this.name() + "_name", component);
        }

        IModel getStoreDescription(Component component) {
            return new ParamResourceModel(this.name() + "_description", component);
        }

        ResourceReference getStoreIcon() {
            return icon;
        }

        Class<? extends Page> getDestinationPage() {
            return destinationPage;
        }

        /**
         * Checks whether the datastore is installed and available (e.g., all the extra libraries it
         * requires are there)
         * 
         * @return
         */
        boolean isAvailable() {
            try {
                Class<?> clazz = Class.forName(factoryClassName);
                DataStoreFactorySpi factory = (DataStoreFactorySpi) clazz.newInstance();
                return factory.isAvailable();
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Returns the list of stores that are known to be available
         * 
         * @return
         * @see #isAvailable()
         */
        static List<Store> getAvailableStores() {
            List<Store> stores = new ArrayList<Store>(Arrays.asList(values()));
            for (Iterator<Store> it = stores.iterator(); it.hasNext();) {
                Store store = it.next();
                if (!store.isAvailable())
                    it.remove();
            }

            return stores;
        }
    }
}
