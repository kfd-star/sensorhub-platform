package com.sensorhub.clientsdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.sensorhub.clientsdk.model.User;


import java.util.HashMap;
import java.util.Map;

import static com.sensorhub.clientsdk.utils.SignUtils.genSign;

/**
 * 调用第三方接口的客户端
 *
 */
public class SensorHubClient {

    private String gatewayHost;

    private String accessKey;

    private String secretKey;

    public SensorHubClient(String accessKey, String secretKey) {
        this(accessKey, secretKey, "http://localhost:8290");
    }

    public SensorHubClient(String accessKey, String secretKey, String gatewayHost) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.gatewayHost = gatewayHost;
    }

    private String gatewayUrl(String path) {
        String host = gatewayHost == null || gatewayHost.trim().isEmpty()
                ? "http://localhost:8290"
                : gatewayHost.trim();
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host + path;
    }

    public String getNameByGet(String name) {
        //可以单独传入http参数，这样参数会自动做URL编码，拼接在URL中
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String json = JSONUtil.toJsonStr(paramMap);
        HttpResponse httpResponse = HttpRequest.get(gatewayUrl("/api/name/get"))
                .form(paramMap)
                .addHeaders(getHeaderMap(json))
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;
    }

    public String getNameByPost(String name) {
        //可以单独传入http参数，这样参数会自动做URL编码，拼接在URL中
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String json = JSONUtil.toJsonStr(paramMap);
        HttpResponse httpResponse = HttpRequest.post(gatewayUrl("/api/name/post"))
                .form(paramMap)
                .addHeaders(getHeaderMap(json))
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;
    }

    private Map<String, String> getHeaderMap(String body) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", accessKey);
        // 一定不能直接发送
//        hashMap.put("secretKey", secretKey);
        hashMap.put("nonce", RandomUtil.randomNumbers(4));
        hashMap.put("body", body);
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        hashMap.put("sign", genSign(body, secretKey));
        return hashMap;
    }

    public String getUsernameByPost(User user) {
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpResponse = HttpRequest.post(gatewayUrl("/api/name/user"))
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;
    }
}
