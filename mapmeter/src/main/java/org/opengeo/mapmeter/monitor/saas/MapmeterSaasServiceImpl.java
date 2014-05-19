package org.opengeo.mapmeter.monitor.saas;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;

public class MapmeterSaasServiceImpl implements MapmeterSaasService {

    private final HttpClient httpClient;

    public MapmeterSaasServiceImpl(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private void closeIdleConnections() {
        httpClient.getHttpConnectionManager().closeIdleConnections(5000);
    }

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

    @Override
    public MapmeterSaasResponse createAnonymousTrial() throws IOException {
        // TODO
        String baseUrl = "http://localhost:3000";
        PostMethod postMethod = new PostMethod(baseUrl + "/api/v2/anonymous-trial");
        try {
            int status = httpClient.executeMethod(postMethod);
            String responseBody = getResponseBody(postMethod);
            Map<String, Object> response = parseJsonResponse(responseBody);
            return new MapmeterSaasResponse(status, response);
        } finally {
            postMethod.releaseConnection();
            closeIdleConnections();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String responseBody) {
        JSONObject jsonObject = JSONObject.fromObject(responseBody);
        return jsonObject;
    }

}
