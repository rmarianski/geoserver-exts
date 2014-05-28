package org.geoserver.web;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.check.ConnectionChecker;
import org.opengeo.mapmeter.monitor.check.ConnectionResult;
import org.opengeo.mapmeter.monitor.config.MapmeterConfiguration;
import org.opengeo.mapmeter.monitor.saas.MapmeterEnableResult;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasException;
import org.opengeo.mapmeter.monitor.saas.MapmeterService;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

public class MapmeterPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logging.getLogger(MapmeterPage.class);

    private final transient MapmeterConfiguration mapmeterConfiguration;

    private final transient ConnectionChecker connectionChecker;

    private final transient MapmeterService mapmeterService;

    private RequiredTextField<String> apiKeyField;

    private Form<?> apiKeyForm;

    public MapmeterPage() {
        GeoServerApplication geoServerApplication = getGeoServerApplication();
        this.mapmeterConfiguration = geoServerApplication.getBeanOfType(MapmeterConfiguration.class);
        if (mapmeterConfiguration == null) {
            throw new IllegalStateException("Error finding MapmeterConfiguration bean");
        }
        this.connectionChecker = geoServerApplication.getBeanOfType(ConnectionChecker.class);
        if (connectionChecker == null) {
            throw new IllegalStateException("Error finding ConnectionChecker bean");
        }
        this.mapmeterService = geoServerApplication.getBeanOfType(MapmeterService.class);
        if (mapmeterService == null) {
            throw new IllegalStateException("Error finding MapmeterSaasService bean");
        }
        addElements();
    }

    private void addElements() {
        Optional<String> maybeApiKey;
        boolean isApiKeyOverridden;
        boolean isOnPremise;
        synchronized (mapmeterConfiguration) {
            maybeApiKey = mapmeterConfiguration.getApiKey();
            isApiKeyOverridden = mapmeterConfiguration.isApiKeyOverridden();
            isOnPremise = mapmeterConfiguration.getIsOnPremise();
        }
        String apiKey = maybeApiKey.or("");

        addApiKeyForm(apiKey);
        WebMarkupContainer apiWarning = addApiKeyEnvWarning(apiKey);
        // addConnectionCheckForm();
        apiKeyForm.setVisible(!isApiKeyOverridden);
        apiWarning.setVisible(isApiKeyOverridden);

        // TODO properly make these conditional
        addMapmeterEnableForm();
        addCredentialsConvertForm();
        addCredentialsSaveForm();
    }

    private WebMarkupContainer addApiKeyEnvWarning(String apiKey) {
        WebMarkupContainer apiKeyWarning = new WebMarkupContainer("apikey-warning");
        apiKeyWarning.add(new Label("active-apikey", Model.of(apiKey)));
        add(apiKeyWarning);
        return apiKeyWarning;
    }

    private Form<?> addConnectionCheckForm() {
        final Form<?> connectionCheckForm = new Form<Void>("connection-check-form");

        connectionCheckForm.add(new FeedbackPanel("connection-check-feedback"));

        AjaxLink<?> connectionCheckButton = new IndicatingAjaxLink<Void>("connection-check-button") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                target.addComponent(connectionCheckForm);
                Optional<String> maybeApiKey;
                synchronized (mapmeterConfiguration) {
                    maybeApiKey = mapmeterConfiguration.getApiKey();
                }
                if (maybeApiKey.isPresent()) {
                    ConnectionResult result = connectionChecker.checkConnection(maybeApiKey.get());
                    if (result.isError()) {
                        Optional<Integer> maybeStatusCode = result.getStatusCode();
                        if (maybeStatusCode.isPresent()) {
                            int statusCode = maybeStatusCode.get();
                            if (statusCode == HttpStatus.SC_OK) {
                                connectionCheckForm.error(result.getError());
                            } else {
                                connectionCheckForm.error(statusCode + ": " + result.getError());
                            }
                        } else {
                            connectionCheckForm.error(result.getError());
                        }
                    } else {
                        connectionCheckForm.info("Connection successfully established.");
                    }
                } else {
                    connectionCheckForm.error("Please set an api key first");
                }
            }
        };
        connectionCheckForm.add(connectionCheckButton);

        add(connectionCheckForm);
        return connectionCheckForm;
    }

    public Form<?> addApiKeyForm(String apiKey) {
        apiKeyForm = new Form<Void>("apikey-form");

        apiKeyField = new RequiredTextField<String>("apikey-field", Model.of(apiKey));
        apiKeyField.setOutputMarkupId(true);
        apiKeyForm.add(apiKeyField);

        apiKeyForm.add(new FeedbackPanel("apikey-feedback"));

        AjaxButton apiKeyButton = new IndicatingAjaxButton("apikey-button") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                String apiKey = apiKeyField.getModel().getObject().trim();
                try {
                    save(apiKey);
                    form.info("API key saved");
                } catch (IOException e) {
                    String msg = "Failure saving api key: " + apiKey;
                    LOGGER.severe(msg);
                    LOGGER.severe(e.getLocalizedMessage());
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info(Throwables.getStackTraceAsString(e));
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
        apiKeyForm.add(apiKeyButton);

        add(apiKeyForm);

        return apiKeyForm;
    }

    public Form<?> addMapmeterEnableForm() {
        Form<?> enableMapmeterForm = new Form<Void>("mapmeter-enable-form");
        AjaxButton enableMapmeterButton = new IndicatingAjaxButton("mapmeter-enable-button") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    MapmeterEnableResult mapmeterEnableResult = mapmeterService.startFreeTrial();
                    String apikey = mapmeterEnableResult.getServerApiKey();
                    apiKeyField.getModel().setObject(apikey);
                    target.addComponent(apiKeyField);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } catch (MapmeterSaasException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
        };
        enableMapmeterForm.add(enableMapmeterButton);
        add(enableMapmeterForm);
        return enableMapmeterForm;
    }

    private void addCredentialsConvertForm() {
        Form<?> credentialsConvertForm = new Form<Void>("mapmeter-credentials-convert-form");
        final FeedbackPanel feedbackPanel = new FeedbackPanel(
                "mapmeter-credentials-convert-feedback");
        feedbackPanel.setOutputMarkupId(true);
        IndicatingAjaxButton credentialsConvertButton = new IndicatingAjaxButton(
                "mapmeter-credentials-convert-button") {

            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                form.info("Credentials converted");
                target.addComponent(feedbackPanel);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                super.onError(target, form);
                target.addComponent(feedbackPanel);
            }
        };
        RequiredTextField<String> mapmeterCredentialsUsername = new RequiredTextField<String>(
                "mapmeter-credentials-convert-username", Model.of(""));
        RequiredTextField<String> mapmeterCredentialsPassword1 = new RequiredTextField<String>(
                "mapmeter-credentials-convert-password1", Model.of(""));
        RequiredTextField<String> mapmeterCredentialsPassword2 = new RequiredTextField<String>(
                "mapmeter-credentials-convert-password2", Model.of(""));

        credentialsConvertForm.add(mapmeterCredentialsUsername);
        credentialsConvertForm.add(mapmeterCredentialsPassword1);
        credentialsConvertForm.add(mapmeterCredentialsPassword2);
        credentialsConvertForm.add(feedbackPanel);
        credentialsConvertForm.add(credentialsConvertButton);

        add(credentialsConvertForm);
    }

    private void addCredentialsSaveForm() {
        Form<?> credentialsSaveForm = new Form<Void>("mapmeter-credentials-save-form");
        final FeedbackPanel feedbackPanel = new FeedbackPanel("mapmeter-credentials-save-feedback");
        feedbackPanel.setOutputMarkupId(true);
        IndicatingAjaxButton credentialsSaveButton = new IndicatingAjaxButton(
                "mapmeter-credentials-save-button") {

            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                form.info("Credentials saved");
                target.addComponent(feedbackPanel);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                super.onError(target, form);
                target.addComponent(feedbackPanel);
            }
        };
        RequiredTextField<String> mapmeterCredentialsUsername = new RequiredTextField<String>(
                "mapmeter-credentials-save-username", Model.of(""));
        RequiredTextField<String> mapmeterCredentialsPassword1 = new RequiredTextField<String>(
                "mapmeter-credentials-save-password1", Model.of(""));
        RequiredTextField<String> mapmeterCredentialsPassword2 = new RequiredTextField<String>(
                "mapmeter-credentials-save-password2", Model.of(""));

        credentialsSaveForm.add(mapmeterCredentialsUsername);
        credentialsSaveForm.add(mapmeterCredentialsPassword1);
        credentialsSaveForm.add(mapmeterCredentialsPassword2);
        credentialsSaveForm.add(feedbackPanel);
        credentialsSaveForm.add(credentialsSaveButton);

        add(credentialsSaveForm);
    }

    private void save(String apiKey) throws IOException {
        synchronized (mapmeterConfiguration) {
            mapmeterConfiguration.setApiKey(apiKey);
            mapmeterConfiguration.save();
        }
    }

}
