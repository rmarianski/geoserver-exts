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
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;

public class MapmeterSaasService {

    private String getResponseBody(HttpMethod method) throws IOException {
        Closer closer = Closer.create();
        try {
            InputStream responseBodyAsStream = closer.register(method.getResponseBodyAsStream());
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

    public MapmeterSaasResponse createAnonymousTrial() throws IOException {
        // TODO
        HttpClient httpClient = new HttpClient();
        String baseUrl = "http://localhost:3000";
        PostMethod postMethod = new PostMethod(baseUrl + "/api/v2/anonymous-trial");
        return executeMethod(httpClient, postMethod);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String responseBody) {
        JSONObject jsonObject = JSONObject.fromObject(responseBody);
        return jsonObject;
    }

    public MapmeterSaasResponse fetchData(MapmeterSaasCredentials mapmeterSaasCredentials,
            String apiKey, Date start, Date end) throws IOException {
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getState().setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(mapmeterSaasCredentials.getUsername(),
                        mapmeterSaasCredentials.getPassword()));
        // TODO
        String baseUrl = "http://localhost:3000";
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
        System.err.println(mapmeterSaasCredentials.getUsername());
        System.err.println(mapmeterSaasCredentials.getPassword());
        System.err.println(url);
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
                response = parseJsonResponse(responseBody);
            } catch (JSONException e) {
                response = Collections.<String, Object> singletonMap("error", responseBody);
            }
            return new MapmeterSaasResponse(status, response);
        } finally {
            httpMethod.releaseConnection();
            httpClient.getHttpConnectionManager().closeIdleConnections(5000);
        }
    }
}
