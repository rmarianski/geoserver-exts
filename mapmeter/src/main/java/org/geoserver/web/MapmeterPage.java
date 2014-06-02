package org.geoserver.web;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
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
import org.opengeo.mapmeter.monitor.config.MapmeterConfiguration;
import org.opengeo.mapmeter.monitor.saas.MapmeterEnableResult;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasCredentials;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasException;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasUserState;
import org.opengeo.mapmeter.monitor.saas.MapmeterService;

import com.google.common.base.Optional;

public class MapmeterPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logging.getLogger(MapmeterPage.class);

    private final transient MapmeterConfiguration mapmeterConfiguration;

    private final transient ConnectionChecker connectionChecker;

    private final transient MapmeterService mapmeterService;

    private RequiredTextField<String> apiKeyField;

    private Form<?> apiKeyForm;

    private Form<?> enableMapmeterForm;

    private Form<?> credentialsConvertForm;

    private Form<?> credentialsSaveForm;

    private FeedbackPanel feedbackPanel;

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
        Optional<MapmeterSaasCredentials> maybeMapmeterSaasCredentials;
        synchronized (mapmeterConfiguration) {
            maybeApiKey = mapmeterConfiguration.getApiKey();
            isApiKeyOverridden = mapmeterConfiguration.isApiKeyOverridden();
            baseUrl = mapmeterConfiguration.getBaseUrl();
            isOnPremise = mapmeterConfiguration.getIsOnPremise();
            maybeMapmeterSaasCredentials = mapmeterConfiguration.getMapmeterSaasCredentials();
        }
        String apiKey = maybeApiKey.or("");
        String currentUsername = "";

        boolean shouldDisplayMapmeterEnableForm = false;
        boolean shouldDisplayConvertForm = false;
        boolean shouldDisplayCredentialsUpdateForm = false;
        boolean isInvalidMapmeterCredentials = false;

        if (!isOnPremise) {
            if (!maybeApiKey.isPresent()) {
                shouldDisplayMapmeterEnableForm = true;
            } else {
                if (!maybeMapmeterSaasCredentials.isPresent()) {
                    shouldDisplayCredentialsUpdateForm = true;
                } else {
                    try {
                        MapmeterSaasUserState mapmeterSaasUserState = mapmeterService.findUserState();
                        if (mapmeterSaasUserState == MapmeterSaasUserState.ANONYMOUS) {
                            shouldDisplayConvertForm = true;
                        } else {
                            shouldDisplayCredentialsUpdateForm = true;
                            MapmeterSaasCredentials mapmeterSaasCredentials = maybeMapmeterSaasCredentials.get();
                            currentUsername = mapmeterSaasCredentials.getUsername();
                        }
                    } catch (Exception e) {
                        shouldDisplayCredentialsUpdateForm = true;
                        isInvalidMapmeterCredentials = true;
                    }
                }
            }
        }

        feedbackPanel = new FeedbackPanel("mapmeter-feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        addApiKeyForm(apiKey);
        WebMarkupContainer apiWarning = addApiKeyEnvWarning(apiKey);
        // addConnectionCheckForm();
        apiKeyForm.setVisible(!isApiKeyOverridden);
        apiWarning.setVisible(isApiKeyOverridden);

        addMapmeterEnableForm(baseUrl);
        addCredentialsConvertForm(baseUrl);
        addCredentialsSaveForm(isInvalidMapmeterCredentials, currentUsername);

        enableMapmeterForm.setOutputMarkupId(true);
        enableMapmeterForm.setOutputMarkupPlaceholderTag(true);
        credentialsConvertForm.setOutputMarkupId(true);
        credentialsConvertForm.setOutputMarkupPlaceholderTag(true);
        credentialsSaveForm.setOutputMarkupId(true);
        credentialsSaveForm.setOutputMarkupPlaceholderTag(true);

        enableMapmeterForm.setVisible(shouldDisplayMapmeterEnableForm);
        credentialsConvertForm.setVisible(shouldDisplayConvertForm);
        credentialsSaveForm.setVisible(shouldDisplayCredentialsUpdateForm);

        // to aid in testing
        // final Form<?> testingForm = new Form<Void>("testing");
        // AjaxLink<?> testingLink = new AjaxLink<String>("testing-button") {
        //
        // /** serialVersionUID */
        // private static final long serialVersionUID = 1L;
        //
        // @Override
        // public void onClick(AjaxRequestTarget target) {
        // mapmeterConfiguration.clearConfigForTesting();
        // try {
        // mapmeterConfiguration.save();
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        // target.addComponent(feedbackPanel);
        // setFeedbackInfo("Cleared", target);
        // }
        // };
        // testingForm.add(testingLink);
        // add(testingForm);
    }

    private WebMarkupContainer addApiKeyEnvWarning(String apiKey) {
        WebMarkupContainer apiKeyWarning = new WebMarkupContainer("apikey-warning");
        apiKeyWarning.add(new Label("active-apikey", Model.of(apiKey)));
        add(apiKeyWarning);
        return apiKeyWarning;
    }

    // private Form<?> addConnectionCheckForm() {
    // final Form<?> connectionCheckForm = new Form<Void>("connection-check-form");
    //
    // AjaxLink<?> connectionCheckButton = new IndicatingAjaxLink<Void>("connection-check-button") {
    //
    // private static final long serialVersionUID = 1L;
    //
    // @Override
    // public void onClick(AjaxRequestTarget target) {
    // target.addComponent(connectionCheckForm);
    // Optional<String> maybeApiKey;
    // synchronized (mapmeterConfiguration) {
    // maybeApiKey = mapmeterConfiguration.getApiKey();
    // }
    // if (maybeApiKey.isPresent()) {
    // ConnectionResult result = connectionChecker.checkConnection(maybeApiKey.get());
    // if (result.isError()) {
    // Optional<Integer> maybeStatusCode = result.getStatusCode();
    // if (maybeStatusCode.isPresent()) {
    // int statusCode = maybeStatusCode.get();
    // if (statusCode == HttpStatus.SC_OK) {
    // setFeedbackError(result.getError(), target);
    // } else {
    // setFeedbackError(statusCode + ": " + result.getError(), target);
    // }
    // } else {
    // setFeedbackError(result.getError(), target);
    // }
    // } else {
    // setFeedbackInfo("Connection successfully established.", target);
    // }
    // } else {
    // setFeedbackError("Please set an api key first", target);
    // }
    // }
    // };
    // connectionCheckForm.add(connectionCheckButton);
    //
    // add(connectionCheckForm);
    // return connectionCheckForm;
    // }

    public Form<?> addApiKeyForm(String apiKey) {
        apiKeyForm = new Form<Void>("apikey-form");

        apiKeyField = new RequiredTextField<String>("apikey-field", Model.of(apiKey));
        apiKeyField.setOutputMarkupId(true);
        apiKeyForm.add(apiKeyField);

        AjaxButton apiKeyButton = new IndicatingAjaxButton("apikey-button") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                String apiKey = apiKeyField.getModel().getObject().trim();
                try {
                    synchronized (mapmeterConfiguration) {
                        mapmeterConfiguration.setApiKey(apiKey);
                        mapmeterConfiguration.save();
                    }
                    setFeedbackInfo("API key saved", target);
                } catch (IOException e) {
                    String msg = "Failure saving api key: " + apiKey;
                    LOGGER.log(Level.SEVERE, msg, e);
                    setFeedbackError(msg, target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedbackPanel);
            }

        };
        apiKeyForm.add(apiKeyButton);

        add(apiKeyForm);

        return apiKeyForm;
    }

    private Form<?> addMapmeterEnableForm(String baseUrl) {
        enableMapmeterForm = new Form<Void>("mapmeter-enable-form");
        AjaxButton enableMapmeterButton = new IndicatingAjaxButton("mapmeter-enable-button") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    MapmeterEnableResult mapmeterEnableResult = mapmeterService.startFreeTrial();
                    String apikey = mapmeterEnableResult.getServerApiKey();
                    apiKeyField.getModel().setObject(apikey);
                    setFeedbackInfo("Mapmeter trial activated", target);
                    enableMapmeterForm.setVisible(false);
                    credentialsConvertForm.setVisible(true);
                    target.addComponent(apiKeyField);
                    target.addComponent(enableMapmeterForm);
                    target.addComponent(credentialsConvertForm);
                } catch (IOException e) {
                    setFeedbackError("IO Error activating mapmeter: " + e.getLocalizedMessage(),
                            target);
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    target.addComponent(feedbackPanel);
                } catch (MapmeterSaasException e) {
                    setFeedbackError("Error activating mapmeter: " + e.getLocalizedMessage(),
                            target);
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                    target.addComponent(feedbackPanel);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedbackPanel);
            }
        };
        enableMapmeterForm.add(new Label("mapmeter-enable-baseurl", baseUrl));
        enableMapmeterForm.add(enableMapmeterButton);
        add(enableMapmeterForm);
        return enableMapmeterForm;
    }

    private void addCredentialsConvertForm(String baseUrl) {
        credentialsConvertForm = new Form<Void>("mapmeter-credentials-convert-form");

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
                    setFeedbackError("Passwords do not match", target);
                    return;
                }
                MapmeterSaasCredentials newCredentials = new MapmeterSaasCredentials(username,
                        password2);
                try {
                    mapmeterService.convertMapmeterCredentials(newCredentials);
                    credentialsConvertForm.setVisible(false);
                    credentialsSaveForm.setVisible(true);
                    setFeedbackInfo("Mapmeter credentials converted", target);
                    target.addComponent(credentialsConvertForm);
                    target.addComponent(credentialsSaveForm);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    setFeedbackError("Error converting mapmeter credentials: " + e.getMessage(),
                            target);
                } catch (MapmeterSaasException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                    Optional<String> maybeErrorMessage = e.getErrorMessage();
                    String errorMessageToDisplay;
                    if (maybeErrorMessage.isPresent()) {
                        errorMessageToDisplay = e.getMessage() + ": " + maybeErrorMessage.get();
                    } else {
                        errorMessageToDisplay = "Error response from mapmeter: " + e.getMessage();
                    }
                    setFeedbackError(errorMessageToDisplay, target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedbackPanel);
            }
        };

        credentialsConvertForm.add(new Label("mapmeter-credentials-convert-baseurl", baseUrl));
        credentialsConvertForm.add(mapmeterCredentialsUsername);
        credentialsConvertForm.add(mapmeterCredentialsPassword1);
        credentialsConvertForm.add(mapmeterCredentialsPassword2);
        credentialsConvertForm.add(credentialsConvertButton);

        add(credentialsConvertForm);
    }

    private void addCredentialsSaveForm(boolean isInvalidMapmeterCredentials, String currentUsername) {
        credentialsSaveForm = new Form<Void>("mapmeter-credentials-save-form");

        final RequiredTextField<String> mapmeterCredentialsUsername = new RequiredTextField<String>(
                "mapmeter-credentials-save-username", Model.of(currentUsername));
        final PasswordTextField mapmeterCredentialsPassword1 = new PasswordTextField(
                "mapmeter-credentials-save-password1", Model.of(""));
        final PasswordTextField mapmeterCredentialsPassword2 = new PasswordTextField(
                "mapmeter-credentials-save-password2", Model.of(""));

        WebMarkupContainer invalidCredentialsLabel = new WebMarkupContainer(
                "mapmeter-credentials-save-invalid-credentials");
        invalidCredentialsLabel.setVisible(isInvalidMapmeterCredentials);
        credentialsSaveForm.add(invalidCredentialsLabel);

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
                    setFeedbackError("Passwords do not match", target);
                    return;
                }
                MapmeterSaasCredentials mapmeterSaasCredentials = new MapmeterSaasCredentials(
                        username, password2);
                synchronized (mapmeterConfiguration) {
                    mapmeterConfiguration.setMapmeterSaasCredentials(mapmeterSaasCredentials);
                    try {
                        mapmeterConfiguration.save();
                        setFeedbackInfo("Credentials saved", target);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        setFeedbackError(
                                "Failure saving mapmeter credentials: " + e.getLocalizedMessage(),
                                target);
                    }
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedbackPanel);
            }
        };
        credentialsSaveForm.add(mapmeterCredentialsUsername);
        credentialsSaveForm.add(mapmeterCredentialsPassword1);
        credentialsSaveForm.add(mapmeterCredentialsPassword2);
        credentialsSaveForm.add(credentialsSaveButton);

        add(credentialsSaveForm);
    }

    private void setFeedbackError(String msg, AjaxRequestTarget target) {
        Session.get().cleanupFeedbackMessages();
        feedbackPanel.error(msg);
        target.addComponent(feedbackPanel);
    }

    private void setFeedbackInfo(String msg, AjaxRequestTarget target) {
        Session.get().cleanupFeedbackMessages();
        feedbackPanel.info(msg);
        target.addComponent(feedbackPanel);
    }

}
