package com.example.httpUtil;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @date 2021/12/29 10:09
 */
public class ResultMatcher {
    private final CloseableHttpResponse responseEntity;
    private String body;

    public ResultMatcher(CloseableHttpResponse responseEntity) throws MatchFailedException {
        this.responseEntity = responseEntity;
        if (responseEntity == null) {
            throw new MatchFailedException ("responseEntity = null");
        }
    }

    public ResultMatcher status(int code) throws MatchFailedException {
        int statusCode = responseEntity.getStatusLine ().getStatusCode ();
        if (statusCode != code) {
            throw new MatchFailedException ("statusCode = " + statusCode);
        } else return this;
    }

    public void printBody() throws MatchFailedException {
        hasBody ();
        System.out.println ();
    }

    public String getBody() throws MatchFailedException {
        hasBody ();
        return body;
    }


    public ResultMatcher isOk() throws MatchFailedException {
        return status (200);
    }

    public void bodyEquals(String s) throws MatchFailedException {
        hasBody ();
        if (!body.equals (s)) {
            throw new MatchFailedException ("content = " + body);
        }
    }

    public ResultMatcher hasBody() throws MatchFailedException {
        try {
            body = EntityUtils.toString (responseEntity.getEntity ());
        } catch (IOException e) {
            e.printStackTrace ();
            throw new MatchFailedException ();
        }

        if (body == null || body.trim ().length () == 0) {
            throw new MatchFailedException ();
        }
        return this;
    }

    public ResultMatcher hasHeader(String key, String value) throws MatchFailedException {
        Header[] headers = responseEntity.getHeaders (key);
        boolean ok = false;
        if (headers != null) {
            for (Header header : headers) {
                if (header.getValue ().equals (value)) {
                    ok = true;
                }
            }
        }
        if (ok) {
            return this;
        } else {
            throw new MatchFailedException ();
        }
    }

    private String readEntity() throws MatchFailedException {
        try {
            HttpEntity entity = responseEntity.getEntity ();
            byte[] buf = new byte[(int) entity.getContentLength ()];
            IOUtils.readFully (entity.getContent (), buf);
            return new String (buf);
        } catch (IOException e) {
            e.printStackTrace ();
            throw new MatchFailedException ();
        }
    }
}
