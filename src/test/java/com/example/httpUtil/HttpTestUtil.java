package com.example.httpUtil;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

public final class HttpTestUtil {

    private static HttpRequestBase method(String method) {
        switch (method.toLowerCase ()) {
            case "get":
                return new HttpGet ();
            case "post":
                return new HttpPost ();
            default:
                throw new AssertionError ();
        }
    }

    public static ResultMatcher get(String url, Map<String, String> param,
                                    Map<String, String> headers) throws IOException, URISyntaxException, MatchFailedException {
        return request (url, param, "get", null, headers);
    }

    public static ResultMatcher get(String url) throws IOException, URISyntaxException, MatchFailedException {
        return request (url, null, "get", null, null);
    }

    public static ResultMatcher get(String url, Map<String, String> param) throws IOException, URISyntaxException, MatchFailedException {
        return request (url, param, "get", null, null);
    }

    public static ResultMatcher post(String url, Map<String, String> param,
                                     Map<String, String> headers) throws IOException, URISyntaxException, MatchFailedException {
        return request (url, param, "post", null, headers);
    }

    public static ResultMatcher postBody(String url, Map<String, String> param,
                                         Map<String, String> headers, String body) throws IOException, URISyntaxException, MatchFailedException {
        return request (url, param, "post", body, headers);
    }

    private static ResultMatcher request(String url, Map<String, String> param, String method,
                                         String body, Map<String, String> headers) throws IOException, URISyntaxException, MatchFailedException {
        Objects.requireNonNull (url);
        Objects.requireNonNull (method);
        if (body != null && method.equals ("get")) {
            throw new IllegalArgumentException ("no get");
        }

        CloseableHttpClient httpClient = HttpClientBuilder.create ().build ();
        HttpRequestBase requestBase = method (method);

        URIBuilder uriBuilder = new URIBuilder (url);
        if (param != null) {
            param.forEach (uriBuilder::addParameter);
        }
        requestBase.setURI (uriBuilder.build ());

        if (headers != null) {
            headers.forEach (requestBase::setHeader);
        }

        if (body != null) {
            ((HttpPost) requestBase).setEntity (new StringEntity (body));
        }

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute (requestBase);
            return new ResultMatcher (response);
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close ();
                }
                if (response != null) {
                    response.close ();
                }
            } catch (IOException e) {
                e.printStackTrace ();
            }
        }
    }
}
