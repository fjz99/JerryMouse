package com.example.httpUtil;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @date 2021/12/29 10:40
 */
public class TestUtil {
    @Test
    void get() throws MatchFailedException, IOException, URISyntaxException {
        Map<String, String> map = new HashMap<> ();
        map.put ("text", "%091%20vulnerability");
        HttpTestUtil.get ("https://www.baidu.com/?sl=en&tl=zh-CN&op=translate", map)
                .status (200)
                .printBody ();
    }

    /**
     * GET---无参测试
     *
     * @date 2018年7月13日 下午4:18:50
     */
    @Test
    public void doGetTestOne() {
        // 获得Http客户端(可以理解为:你得先有一个浏览器;注意:实际上HttpClient与浏览器是不一样的)
        CloseableHttpClient httpClient = HttpClientBuilder.create ().build ();
        // 创建Get请求
        HttpGet httpGet = new HttpGet ("http://www.baidu.com");

        // 响应模型
        CloseableHttpResponse response = null;
        try {
            // 由客户端执行(发送)Get请求
            response = httpClient.execute (httpGet);
            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity ();
            System.out.println ("响应状态为:" + response.getStatusLine ());
            if (responseEntity != null) {
                System.out.println ("响应内容长度为:" + responseEntity.getContentLength ());
                System.out.println ("响应内容为:" + EntityUtils.toString (responseEntity));
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace ();
        } finally {
            try {
                // 释放资源
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
