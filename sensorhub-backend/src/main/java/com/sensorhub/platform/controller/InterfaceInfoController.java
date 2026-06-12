package com.sensorhub.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sensorhub.platform.annotation.AuthCheck;
import com.sensorhub.platform.common.BaseResponse;
import com.sensorhub.platform.common.DeleteRequest;
import com.sensorhub.platform.common.ErrorCode;
import com.sensorhub.platform.common.IdRequest;
import com.sensorhub.platform.common.ResultUtils;
import com.sensorhub.platform.constant.CommonConstant;
import com.sensorhub.platform.exception.BusinessException;
import com.sensorhub.platform.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.sensorhub.platform.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.sensorhub.platform.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.sensorhub.platform.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.sensorhub.platform.model.enums.InterfaceInfoStatusEnum;
import com.sensorhub.platform.service.InterfaceInfoService;
import com.sensorhub.platform.service.UserService;
import com.sensorhub.clientsdk.client.SensorHubClient;
import com.sensorhub.clientsdk.utils.SignUtils;
import com.sensorhub.common.model.entity.InterfaceInfo;
import com.sensorhub.common.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Interface management controller.
 */
@RestController
@RequestMapping("/interfaceInfo")
@Slf4j
public class InterfaceInfoController {

    @Value("${sensorhub.client.gateway-host:http://localhost:8290}")
    private String gatewayHost;

    private final Gson gson = new Gson();

    private final RestTemplate restTemplate = new RestTemplate();

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private UserService userService;

    @Resource
    private SensorHubClient sensorHubClient;

    @PostMapping("/add")
    public BaseResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceInfoAddRequest,
                                               HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoAddRequest, interfaceInfo);
        interfaceInfoService.validInterfaceInfo(interfaceInfo, true);
        User loginUser = userService.getLoginUser(request);
        interfaceInfo.setUserId(loginUser.getId());
        boolean result = interfaceInfoService.save(interfaceInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(interfaceInfo.getId());
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest,
                                                     HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = interfaceInfoService.removeById(id);
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceInfoUpdateRequest,
                                                     HttpServletRequest request) {
        if (interfaceInfoUpdateRequest == null || interfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);
        interfaceInfoService.validInterfaceInfo(interfaceInfo, false);
        User user = userService.getLoginUser(request);
        long id = interfaceInfoUpdateRequest.getId();
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    @GetMapping("/get")
    public BaseResponse<InterfaceInfo> getInterfaceInfoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        return ResultUtils.success(interfaceInfo);
    }

    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<InterfaceInfo>> listInterfaceInfo(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        if (interfaceInfoQueryRequest != null) {
            BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        List<InterfaceInfo> interfaceInfoList = interfaceInfoService.list(queryWrapper);
        return ResultUtils.success(interfaceInfoList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceInfoQueryRequest,
                                                                     HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        long current = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        String description = interfaceInfoQuery.getDescription();
        interfaceInfoQuery.setDescription(null);
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(interfaceInfoPage);
    }

    @PostMapping("/online")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                     HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        com.sensorhub.clientsdk.model.User testUser = new com.sensorhub.clientsdk.model.User();
        testUser.setUsername("test");
        String username = sensorHubClient.getUsernameByPost(testUser);
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "interface validation failed");
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.ONLINE.getValue());
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    @PostMapping("/offline")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> offlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                      HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.OFFLINE.getValue());
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    @PostMapping("/invoke")
    public BaseResponse<Object> invokeInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                    HttpServletRequest request) {
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = interfaceInfoInvokeRequest.getId();
        String userRequestParams = interfaceInfoInvokeRequest.getUserRequestParams();
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (oldInterfaceInfo.getStatus() == InterfaceInfoStatusEnum.OFFLINE.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "interface is offline");
        }
        User loginUser = userService.getLoginUser(request);
        String invokeResult = invokeByGateway(oldInterfaceInfo, loginUser, userRequestParams);
        return ResultUtils.success(invokeResult);
    }

    private String invokeByGateway(InterfaceInfo interfaceInfo, User loginUser, String rawRequestParams) {
        try {
            HttpMethod httpMethod = HttpMethod.resolve(
                    StringUtils.defaultIfBlank(interfaceInfo.getMethod(), "GET").toUpperCase());
            if (httpMethod == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "unsupported method");
            }
            Map<String, Object> requestParamMap = parseRequestParamMap(rawRequestParams);
            String requestBody = buildRequestBody(httpMethod, rawRequestParams, requestParamMap);
            String gatewayUrl = buildGatewayUrl(interfaceInfo.getUrl(), httpMethod, requestParamMap);
            HttpHeaders headers = buildGatewayHeaders(loginUser.getAccessKey(), loginUser.getSecretKey(), requestBody);
            HttpEntity<?> entity = requiresRequestBody(httpMethod)
                    ? new HttpEntity<>(requestBody, headers)
                    : new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(gatewayUrl, httpMethod, entity, String.class);
            return responseEntity.getBody();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("invokeByGateway error", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "invoke failed: " + e.getMessage());
        }
    }

    private HttpHeaders buildGatewayHeaders(String accessKey, String secretKey, String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("accessKey", accessKey);
        headers.add("nonce", String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999)));
        headers.add("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        headers.add("body", requestBody);
        headers.add("sign", SignUtils.genSign(requestBody, secretKey));
        return headers;
    }

    private String buildGatewayUrl(String interfaceUrl, HttpMethod httpMethod, Map<String, Object> requestParamMap) {
        if (StringUtils.isBlank(interfaceUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "interface url is blank");
        }
        URI uri = URI.create(interfaceUrl);
        String path = uri.getRawPath();
        if (StringUtils.isBlank(path)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid interface url");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(normalizeGatewayHost() + path);
        if (!requiresRequestBody(httpMethod)) {
            requestParamMap.forEach((key, value) -> {
                if (value != null) {
                    builder.queryParam(key, value);
                }
            });
        }
        return builder.toUriString();
    }

    private String normalizeGatewayHost() {
        String host = StringUtils.defaultIfBlank(gatewayHost, "http://localhost:8290").trim();
        return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    private Map<String, Object> parseRequestParamMap(String rawRequestParams) {
        if (StringUtils.isBlank(rawRequestParams)) {
            return new HashMap<>();
        }
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> parsedMap = gson.fromJson(rawRequestParams, mapType);
            return parsedMap == null ? new HashMap<>() : parsedMap;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String buildRequestBody(HttpMethod httpMethod, String rawRequestParams, Map<String, Object> requestParamMap) {
        if (requiresRequestBody(httpMethod)) {
            return StringUtils.isBlank(rawRequestParams) ? "{}" : rawRequestParams;
        }
        return requestParamMap.isEmpty() ? "{}" : gson.toJson(requestParamMap);
    }

    private boolean requiresRequestBody(HttpMethod httpMethod) {
        return HttpMethod.POST.equals(httpMethod)
                || HttpMethod.PUT.equals(httpMethod)
                || HttpMethod.PATCH.equals(httpMethod);
    }
}
