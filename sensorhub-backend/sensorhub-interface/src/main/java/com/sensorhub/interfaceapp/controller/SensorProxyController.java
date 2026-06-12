package com.sensorhub.interfaceapp.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Proxy controller that exposes local sensor interfaces through
 * sensorhub-interface so they can be published by SensorHub Open Platform.
 */
@RestController
@RequestMapping("/sensor")
public class SensorProxyController {

    @Value("${platform.base-url:http://localhost:7529/api}")
    private String platformBaseUrl;

    @GetMapping(value = "/devices", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getDevices(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/devices", request, response);
    }

    @GetMapping(value = "/gps-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getGpsData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/gps-data", request, response);
    }

    @PostMapping(value = "/gps-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postGpsData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/gps-data", request, response);
    }

    @GetMapping(value = "/esp32-weather-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getEsp32WeatherData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/esp32-weather-data", request, response);
    }

    @PostMapping(value = "/esp32-weather-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postEsp32WeatherData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/esp32-weather-data", request, response);
    }

    @GetMapping(value = "/weather-station-1-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getWeatherStation1Data(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/weather-station-1-data", request, response);
    }

    @PostMapping(value = "/weather-station-1-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postWeatherStation1Data(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/weather-station-1-data", request, response);
    }

    @GetMapping(value = "/weather-station-2-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getWeatherStation2Data(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/weather-station-2-data", request, response);
    }

    @PostMapping(value = "/weather-station-2-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postWeatherStation2Data(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/weather-station-2-data", request, response);
    }

    @GetMapping(value = "/wind-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getWindData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/wind-data", request, response);
    }

    @PostMapping(value = "/wind-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postWindData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/wind-data", request, response);
    }

    @GetMapping(value = "/wind-ocr-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getWindOcrData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/wind-ocr-data", request, response);
    }

    @PostMapping(value = "/wind-ocr-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postWindOcrData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/wind-ocr-data", request, response);
    }

    @GetMapping(value = "/image-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getImageData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/image-data", request, response);
    }

    @PostMapping(value = "/image-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postImageData(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/image-data", request, response);
    }

    @GetMapping(value = "/data-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getDataStats(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/data-stats", request, response);
    }

    @GetMapping(value = "/auth/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAuthUser(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/auth/user", request, response);
    }

    @GetMapping(value = "/auth/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAuthVerify(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/auth/verify", request, response);
    }

    @PostMapping(value = "/auth/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postAuthLogin(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/auth/login", request, response);
    }

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getUsers(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet("/sensor/users", request, response);
    }

    @PostMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public String postUsers(HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformPost("/sensor/users", request, response);
    }

    @GetMapping("/export-data")
    public void exportData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        proxyPlatformGetToResponse("/sensor/export-data", request, response);
    }

    private String proxyPlatformGet(String targetPath,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    String... excludedParams) {
        return proxyRequest(platformBaseUrl, Method.GET, targetPath, request, response, excludedParams);
    }

    private String proxyPlatformGet(String targetPath, HttpServletRequest request, HttpServletResponse response) {
        return proxyPlatformGet(targetPath, request, response, new String[0]);
    }

    private void proxyPlatformGetToResponse(String targetPath,
                                            HttpServletRequest request,
                                            HttpServletResponse response,
                                            String... excludedParams) throws IOException {
        proxyRequestToResponse(platformBaseUrl, Method.GET, targetPath, request, response, excludedParams);
    }

    private String proxyPlatformPost(String targetPath, HttpServletRequest request, HttpServletResponse response) {
        return proxyRequest(platformBaseUrl, Method.POST, targetPath, request, response);
    }

    private String proxyRequest(String baseUrl,
                                Method method,
                                String targetPath,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                String... excludedParams) {
        String targetUrl = buildTargetUrl(baseUrl, targetPath, request, excludedParams);
        HttpRequest targetRequest = HttpRequest.of(targetUrl).method(method);
        copyHeaders(request, targetRequest);
        if (Method.POST.equals(method)) {
            try {
                byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
                if (body.length > 0) {
                    targetRequest.body(body);
                }
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return "{\"success\":false,\"message\":\"failed to read request body\"}";
            }
        }
        HttpResponse httpResponse = targetRequest.execute();
        copyResponseMeta(httpResponse, response);
        return httpResponse.body();
    }

    private void proxyRequestToResponse(String baseUrl,
                                        Method method,
                                        String targetPath,
                                        HttpServletRequest request,
                                        HttpServletResponse response,
                                        String... excludedParams) throws IOException {
        String targetUrl = buildTargetUrl(baseUrl, targetPath, request, excludedParams);
        HttpRequest targetRequest = HttpRequest.of(targetUrl).method(method);
        copyHeaders(request, targetRequest);
        if (Method.POST.equals(method)) {
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
            if (body.length > 0) {
                targetRequest.body(body);
            }
        }
        HttpResponse httpResponse = targetRequest.execute();
        copyResponseMeta(httpResponse, response);
        response.getOutputStream().write(httpResponse.bodyBytes());
        response.getOutputStream().flush();
    }

    private void copyResponseMeta(HttpResponse httpResponse, HttpServletResponse response) {
        response.setStatus(httpResponse.getStatus());
        copyResponseHeaderIfPresent(httpResponse, response, "Content-Type");
        copyResponseHeaderIfPresent(httpResponse, response, "Content-Disposition");
        copyResponseHeaderIfPresent(httpResponse, response, "Cache-Control");
        copyResponseHeaderIfPresent(httpResponse, response, "Pragma");
        copyResponseHeaderIfPresent(httpResponse, response, "Expires");
    }

    private void copyResponseHeaderIfPresent(HttpResponse httpResponse,
                                             HttpServletResponse response,
                                             String headerName) {
        String headerValue = httpResponse.header(headerName);
        if (isNotBlank(headerValue)) {
            response.setHeader(headerName, headerValue);
        }
    }

    private String buildTargetUrl(String baseUrl,
                                  String targetPath,
                                  HttpServletRequest request,
                                  String... excludedParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + targetPath);
        request.getParameterMap().forEach((key, values) -> {
            if (shouldSkipParam(key, excludedParams)) {
                return;
            }
            if (values == null) {
                return;
            }
            for (String value : values) {
                builder.queryParam(key, value);
            }
        });
        return builder.toUriString();
    }

    private boolean shouldSkipParam(String key, String... excludedParams) {
        if (excludedParams == null) {
            return false;
        }
        for (String excludedParam : excludedParams) {
            if (equalsText(key, excludedParam)) {
                return true;
            }
        }
        return false;
    }

    private void copyHeaders(HttpServletRequest request, HttpRequest targetRequest) {
        copyHeaderIfPresent(request, targetRequest, "Authorization");
        copyHeaderIfPresent(request, targetRequest, "Cookie");
        copyHeaderIfPresent(request, targetRequest, "Content-Type");
        copyHeaderIfPresent(request, targetRequest, "Accept");
    }

    private void copyHeaderIfPresent(HttpServletRequest request, HttpRequest targetRequest, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (isNotBlank(headerValue)) {
            targetRequest.header(headerName, headerValue);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    private boolean equalsText(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
