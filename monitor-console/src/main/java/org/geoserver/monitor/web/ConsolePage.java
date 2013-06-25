package org.geoserver.monitor.web;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.util.logging.Logging;
import org.opengeo.console.monitor.transport.ConsoleMessageTransportConfig;

import com.google.common.base.Throwables;

public class ConsolePage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(ConsolePage.class);

    private final transient ConsoleMessageTransportConfig messageTransportConfig;

    public ConsolePage() {
        GeoServerApplication geoServerApplication = getGeoServerApplication();
        this.messageTransportConfig = geoServerApplication.getBeanOfType(ConsoleMessageTransportConfig.class);
        addElements();
    }

    private void addElements() {
        Form<?> form = new Form<Void>("apikey-form");

        String apiKey = messageTransportConfig.getApiKey().or("");
        final RequiredTextField<String> apiKeyField = new RequiredTextField<String>("apikey",
                Model.of(apiKey));
        form.add(apiKeyField);

        form.add(new FeedbackPanel("feedback"));

        AjaxButton ajaxButton = new AjaxButton("apikey-button") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                String apiKey = apiKeyField.getModelObject().trim();
                try {
                    save(apiKey);
                    form.info("Api key saved: '" + apiKey + "'. Please restart GeoServer to apply these changes.");
                } catch (IOException e) {
                    String msg = "Failure saving api key: " + apiKey;
                    LOGGER.severe(msg);
                    LOGGER.severe(e.getLocalizedMessage());
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(Throwables.getStackTraceAsString(e));
                    }
                    form.error(msg);
                }
                target.addComponent(form);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(form);
            }

        };
        form.add(ajaxButton);

        add(form);
    }

    private void save(String apiKey) throws IOException {
        messageTransportConfig.setApiKey(apiKey);
        messageTransportConfig.save();
    }

}
