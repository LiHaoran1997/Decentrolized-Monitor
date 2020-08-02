package springcloud.hellospringcloudalibabanacosmonitor.service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.nacos.client.naming.utils.IoUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

public class HttpClient {
    public static class HttpResult {
        final public int code;
        final public String content;
        final private Map<String, String> respHeaders;

        public HttpResult(int code, String content, Map<String, String> respHeaders) {
            this.code = code;
            this.content = content;
            this.respHeaders = respHeaders;
        }

        public String getHeader(String name) {
            return respHeaders.get(name);
        }
    }

    public static HttpResult fabricPostLarge(String url, Map<String, String> headers, String content) {
        HttpClientBuilder builder = HttpClients.custom();
        CloseableHttpClient httpClient = builder.build();
        HttpResponse response=null;
        try {
            builder.setConnectionTimeToLive(500, TimeUnit.MILLISECONDS);
            HttpPost httpost = new HttpPost(url);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpost.setHeader(entry.getKey(), entry.getValue());
            }
            httpost.setEntity(new StringEntity(content, ContentType.create("application/json", "UTF-8")));
            response = (HttpResponse) httpClient.execute(httpost);
            HttpEntity entity = response.getEntity();
            HeaderElement[] headerElements = entity.getContentType().getElements();
            String charset = headerElements[0].getParameterByName("charset").getValue();

            return new HttpResult(response.getStatusLine().getStatusCode(),
                    IoUtils.toString(entity.getContent(), charset), Collections.<String, String>emptyMap());
        } catch (Exception e) {
            return new HttpResult(500, e.toString(), Collections.<String, String>emptyMap());
        }
    }
}
