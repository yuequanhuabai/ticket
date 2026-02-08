package com.ticket.util;

import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpUtil {

    private static final OkHttpClient CLIENT;
    private static long lastRequestTime = 0;
    private static final long MIN_INTERVAL_MS = 1000;

    /** 简单的内存 Cookie 存储 */
    private static final Map<String, List<Cookie>> COOKIE_STORE = new HashMap<>();

    static {
        CookieJar cookieJar = new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                COOKIE_STORE.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = COOKIE_STORE.get(url.host());
                return cookies != null ? cookies : new ArrayList<>();
            }
        };

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true);

        try {
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new java.security.SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            System.err.println("警告: SSL 配置失败，使用默认配置: " + e.getMessage());
        }

        CLIENT = builder.build();
    }

    /**
     * 初始化会话 - 先访问12306页面获取必要的Cookie
     */
    public static void initSession() throws IOException {
        throttle();
        Request request = new Request.Builder()
                .url("https://kyfw.12306.cn/otn/leftTicket/init")
                .header("User-Agent", getUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            // 只需要拿到 Cookie，内容不重要
            if (response.body() != null) {
                response.body().close();
            }
        }
    }

    /**
     * 发送 GET 请求到 12306 API
     */
    public static String get(String url) throws IOException {
        throttle();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", getUserAgent())
                .header("Referer", "https://kyfw.12306.cn/otn/leftTicket/init")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Connection", "keep-alive")
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败, HTTP状态码: " + response.code() + ", URL: " + url);
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * 发送 GET 请求并返回 HTML 内容（用于解析页面）
     */
    public static String getHtml(String url) throws IOException {
        throttle();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", getUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败, HTTP状态码: " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    private static String getUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    }

    private static synchronized void throttle() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < MIN_INTERVAL_MS) {
            try {
                Thread.sleep(MIN_INTERVAL_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
