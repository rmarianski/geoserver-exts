package org.opengeo.mapmeter.monitor.saas;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;

public class MapmeterSaasService {

    private String getResponseBody(HttpMethod method) throws IOException {
        Closer closer = Closer.create();
        try {
            InputStream responseBodyAsStream = method.getResponseBodyAsStream();
            if (responseBodyAsStream == null) {
                return null;
            }
            responseBodyAsStream = closer.register(responseBodyAsStream);
            InputStreamReader inputStreamReader = closer.register(new InputStreamReader(
                    responseBodyAsStream, Charsets.UTF_8));
            String responseBodyAsString = CharStreams.toString(inputStreamReader);
            return responseBodyAsString;
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    private HttpClient createEphemeralHttpClient() {
        // TODO make this into a spring bean and re-use these timeouts across all http clients
        HttpConnectionManagerParams httpConnectionManagerParams = new HttpConnectionManagerParams();
        int connectionTimeoutMillis = 5000;
        httpConnectionManagerParams.setConnectionTimeout(connectionTimeoutMillis);
        httpConnectionManagerParams.setSoTimeout(connectionTimeoutMillis);

        SimpleHttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
        connectionManager.setParams(httpConnectionManagerParams);

        HttpClient httpClient = new HttpClient(connectionManager);
        return httpClient;
    }

    private void addBasicAuth(HttpClient httpClient, MapmeterSaasCredentials mapmeterSaasCredentials) {
        httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getState().setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(mapmeterSaasCredentials.getUsername(),
                        mapmeterSaasCredentials.getPassword()));
    }

    public MapmeterSaasResponse createAnonymousTrial(String baseUrl) throws IOException {
        HttpClient httpClient = createEphemeralHttpClient();
        PostMethod postMethod = new PostMethod(baseUrl + "/api/v2/anonymous-trial");
        return executeMethod(httpClient, postMethod);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String responseBody) {
        JSONObject jsonObject = JSONObject.fromObject(responseBody);
        return jsonObject;
    }

    public MapmeterSaasResponse fetchData(String baseUrl,
            MapmeterSaasCredentials mapmeterSaasCredentials, String apiKey, Date start, Date end)
            throws IOException {
        HttpClient httpClient = createEphemeralHttpClient();
        addBasicAuth(httpClient, mapmeterSaasCredentials);
        String statsUrl = baseUrl + "/api/v1/stats";
        HttpURL httpURL = new HttpURL(statsUrl);
        String[] queryNames = new String[] { "api_key", "start_time", "end_time", "interval",
                "stats" };
        long startSeconds = start.getTime() / 1000;
        long endSeconds = end.getTime() / 1000;
        String[] queryValues = new String[] { apiKey, "" + startSeconds, "" + endSeconds, "day",
                "request_count" };
        httpURL.setQuery(queryNames, queryValues);
        String url = httpURL.getURI();
        GetMethod getMethod = new GetMethod(url);
        return executeMethod(httpClient, getMethod);
    }

    public MapmeterSaasResponse convertCredentials(String baseUrl, String userId,
            MapmeterSaasCredentials existingMapmeterSaasCredentials,
            MapmeterSaasCredentials newMapmeterSaasCredentials) throws IOException {
        HttpClient httpClient = createEphemeralHttpClient();
        addBasicAuth(httpClient, existingMapmeterSaasCredentials);
        String convertCredentialsUrl = baseUrl + "/api/v2/anonymous-trial/convert-credentials/"
                + userId;
        HttpURL httpURL = new HttpURL(convertCredentialsUrl);
        String url = httpURL.getURI();

        JSONObject payload = new JSONObject();
        payload.put("email", newMapmeterSaasCredentials.getUsername());
        payload.put("password", newMapmeterSaasCredentials.getPassword());

        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestEntity(new StringRequestEntity(payload.toString(), "application/json",
                Charsets.UTF_8.toString()));
        return executeMethod(httpClient, postMethod);
    }

    public MapmeterSaasResponse lookupUser(String baseUrl,
            MapmeterSaasCredentials mapmeterSaasCredentials) throws IOException {
        HttpClient httpClient = createEphemeralHttpClient();
        addBasicAuth(httpClient, mapmeterSaasCredentials);

        String lookupUserUrl = baseUrl + "/api/v2/users/lookup";
        HttpURL httpURL = new HttpURL(lookupUserUrl);
        httpURL.setQuery("email", mapmeterSaasCredentials.getUsername());
        String url = httpURL.getURI();

        GetMethod getMethod = new GetMethod(url);
        return executeMethod(httpClient, getMethod);
    }

    private MapmeterSaasResponse executeMethod(HttpClient httpClient, HttpMethod httpMethod)
            throws IOException, HttpException {
        try {
            int status = httpClient.executeMethod(httpMethod);
            String responseBody = getResponseBody(httpMethod);
            Map<String, Object> response;
            try {
                response = responseBody != null ? parseJsonResponse(responseBody)
                        : Collections.<String, Object> emptyMap();
            } catch (JSONException e) {
                response = Collections.<String, Object> singletonMap("message", responseBody);
            }
            return new MapmeterSaasResponse(status, response);
        } finally {
            httpMethod.releaseConnection();
            httpClient.getHttpConnectionManager().closeIdleConnections(0);
        }
    }

}
