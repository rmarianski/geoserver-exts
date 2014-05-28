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
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Objects;
import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.check.ConnectionChecker;
import org.opengeo.mapmeter.monitor.check.ConnectionResult;
import org.opengeo.mapmeter.monitor.config.MapmeterConfiguration;
import org.opengeo.mapmeter.monitor.saas.MapmeterEnableResult;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasCredentials;
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

    private Form<Void> credentialsConvertForm;

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
        String baseUrl;
        boolean isOnPremise;
        synchronized (mapmeterConfiguration) {
            maybeApiKey = mapmeterConfiguration.getApiKey();
            isApiKeyOverridden = mapmeterConfiguration.isApiKeyOverridden();
            baseUrl = mapmeterConfiguration.getBaseUrl();
            isOnPremise = mapmeterConfiguration.getIsOnPremise();
        }
        String apiKey = maybeApiKey.or("");

        addApiKeyForm(apiKey);
        WebMarkupContainer apiWarning = addApiKeyEnvWarning(apiKey);
        // addConnectionCheckForm();
        apiKeyForm.setVisible(!isApiKeyOverridden);
        apiWarning.setVisible(isApiKeyOverridden);

        // TODO properly make these conditional
        addMapmeterEnableForm(baseUrl);
        addCredentialsConvertForm(baseUrl);
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

    private Form<?> addMapmeterEnableForm(String baseUrl) {
        final Form<?> enableMapmeterForm = new Form<Void>("mapmeter-enable-form");
        final FeedbackPanel feedbackPanel = new FeedbackPanel("mapmeter-enable-feedback");
        feedbackPanel.setOutputMarkupId(true);
        AjaxButton enableMapmeterButton = new IndicatingAjaxButton("mapmeter-enable-button") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    MapmeterEnableResult mapmeterEnableResult = mapmeterService.startFreeTrial();
                    String apikey = mapmeterEnableResult.getServerApiKey();
                    apiKeyField.getModel().setObject(apikey);
                    apiKeyForm.info("Mapmeter trial activated");
                    enableMapmeterForm.setVisible(false);
                    credentialsConvertForm.setVisible(true);
                    target.addComponent(apiKeyField);
                    target.addComponent(apiKeyForm);
                    target.addComponent(enableMapmeterForm);
                    target.addComponent(credentialsConvertForm);
                } catch (IOException e) {
                    form.error("IO Error activating mapmeter: " + e.getLocalizedMessage());
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    target.addComponent(feedbackPanel);
                } catch (MapmeterSaasException e) {
                    form.error("Error activating mapmeter: " + e.getLocalizedMessage());
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    target.addComponent(feedbackPanel);
                }
            }
        };
        enableMapmeterForm.add(new Label("mapmeter-enable-baseurl", baseUrl));
        enableMapmeterForm.add(feedbackPanel);
        enableMapmeterForm.add(enableMapmeterButton);
        add(enableMapmeterForm);
        return enableMapmeterForm;
    }

    private void addCredentialsConvertForm(String baseUrl) {
        credentialsConvertForm = new Form<Void>("mapmeter-credentials-convert-form");
        final FeedbackPanel feedbackPanel = new FeedbackPanel(
                "mapmeter-credentials-convert-feedback");
        feedbackPanel.setOutputMarkupId(true);

        final RequiredTextField<String> mapmeterCredentialsUsername = new RequiredTextField<String>(
                "mapmeter-credentials-convert-username", Model.of(""));
        final PasswordTextField mapmeterCredentialsPassword1 = new PasswordTextField(
                "mapmeter-credentials-convert-password1", Model.of(""));
        final PasswordTextField mapmeterCredentialsPassword2 = new PasswordTextField(
                "mapmeter-credentials-convert-password2", Model.of(""));

        IndicatingAjaxButton credentialsConvertButton = new IndicatingAjaxButton(
                "mapmeter-credentials-convert-button") {

            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedbackPanel);
                String username = mapmeterCredentialsUsername.getModel().getObject().trim();
                String password1 = mapmeterCredentialsPassword1.getModel().getObject().trim();
                String password2 = mapmeterCredentialsPassword2.getModel().getObject().trim();
                if (!Objects.equal(password1, password2)) {
                    form.error("Password fields are not the same");
                    return;
                }
                MapmeterSaasCredentials newCredentials = new MapmeterSaasCredentials(username,
                        password2);
                try {
                    mapmeterService.convertMapmeterCredentials(newCredentials);
                    form.info("Mapmeter credentials converted");
                } catch (IOException e) {
                    form.error("Error converting mapmeter credentials: " + e.getMessage());
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                super.onError(target, form);
                target.addComponent(feedbackPanel);
            }
        };

        credentialsConvertForm.add(new Label("mapmeter-credentials-convert-baseurl", baseUrl));
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

        final RequiredTextField<String> mapmeterCredentialsUsername = new RequiredTextField<String>(
                "mapmeter-credentials-save-username", Model.of(""));
        final PasswordTextField mapmeterCredentialsPassword1 = new PasswordTextField(
                "mapmeter-credentials-save-password1", Model.of(""));
        final PasswordTextField mapmeterCredentialsPassword2 = new PasswordTextField(
                "mapmeter-credentials-save-password2", Model.of(""));

        IndicatingAjaxButton credentialsSaveButton = new IndicatingAjaxButton(
                "mapmeter-credentials-save-button") {

            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedbackPanel);
                String username = mapmeterCredentialsUsername.getModel().getObject().trim();
                String password1 = mapmeterCredentialsPassword1.getModel().getObject().trim();
                String password2 = mapmeterCredentialsPassword2.getModel().getObject().trim();
                if (!Objects.equal(password1, password2)) {
                    form.error("Password fields are not the same");
                    return;
                }
                MapmeterSaasCredentials mapmeterSaasCredentials = new MapmeterSaasCredentials(
                        username, password2);
                synchronized (mapmeterConfiguration) {
                    mapmeterConfiguration.setMapmeterSaasCredentials(mapmeterSaasCredentials);
                    try {
                        mapmeterConfiguration.save();
                        form.info("Credentials saved");
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        form.error("Failure saving mapmeter credentials: "
                                + e.getLocalizedMessage());
                    }
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                super.onError(target, form);
                target.addComponent(feedbackPanel);
            }
        };
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
