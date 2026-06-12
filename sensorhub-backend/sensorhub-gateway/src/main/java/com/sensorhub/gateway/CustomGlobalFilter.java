package com.sensorhub.gateway;

import com.sensorhub.clientsdk.utils.SignUtils;
import com.sensorhub.common.model.entity.InterfaceInfo;
import com.sensorhub.common.model.entity.User;
import com.sensorhub.common.service.InnerInterfaceInfoService;
import com.sensorhub.common.service.InnerUserInterfaceInfoService;
import com.sensorhub.common.service.InnerUserService;
import com.sensorhub.common.support.SensorRouteAliasSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Global gateway filter for authentication and invoke statistics.
 */
@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @DubboReference(check = false)
    private InnerUserService innerUserService;

    @DubboReference(check = false)
    private InnerInterfaceInfoService innerInterfaceInfoService;

    @DubboReference(check = false)
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    private static final List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    @Value("${sensorhub.gateway.interface-host:http://localhost:8123}")
    private String interfaceHost;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String originalPath = request.getPath().value();
        String gatewayPath = SensorRouteAliasSupport.canonicalizeApiPath(originalPath);
        String interfacePath = toInterfaceServicePath(gatewayPath);
        String path = interfaceHost + gatewayPath;
        String method = request.getMethod() == null ? HttpMethod.GET.name() : request.getMethod().name();
        String sourceAddress = request.getRemoteAddress() == null
                ? ""
                : request.getRemoteAddress().getAddress().getHostAddress();

        log.info("请求唯一标识：{}", request.getId());
        log.info("请求路径：{}", path);
        log.info("请求方法：{}", method);
        log.info("请求参数：{}", request.getQueryParams());
        log.info("请求来源地址：{}", sourceAddress);
        log.info("请求来源地址：{}", request.getRemoteAddress());

        ServerHttpResponse response = exchange.getResponse();
        if (!IP_WHITE_LIST.contains(sourceAddress)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String nonce = headers.getFirst("nonce");
        String timestamp = headers.getFirst("timestamp");
        String sign = headers.getFirst("sign");
        String body = headers.getFirst("body");
        if (accessKey == null || nonce == null || timestamp == null || sign == null || body == null) {
            return handleNoAuth(response);
        }

        User invokeUser;
        try {
            invokeUser = innerUserService.getInvokeUser(accessKey);
        } catch (Exception e) {
            log.error("getInvokeUser error", e);
            return handleNoAuth(response);
        }
        if (invokeUser == null) {
            return handleNoAuth(response);
        }

        long nonceValue;
        long timestampValue;
        try {
            nonceValue = Long.parseLong(nonce);
            timestampValue = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return handleNoAuth(response);
        }
        if (nonceValue > 10000L) {
            return handleNoAuth(response);
        }

        long currentTime = System.currentTimeMillis() / 1000;
        long fiveMinutes = 60 * 5L;
        if ((currentTime - timestampValue) >= fiveMinutes) {
            return handleNoAuth(response);
        }

        String secretKey = invokeUser.getSecretKey();
        String serverSign = SignUtils.genSign(body, secretKey);
        if (!sign.equals(serverSign)) {
            return handleNoAuth(response);
        }

        InterfaceInfo interfaceInfo;
        try {
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
        } catch (Exception e) {
            log.error("getInterfaceInfo error", e);
            return handleNoAuth(response);
        }
        if (interfaceInfo == null) {
            return handleNoAuth(response);
        }

        ServerHttpRequest routedRequest = request.mutate().path(interfacePath).build();
        return handleResponse(exchange.mutate().request(routedRequest).build(), chain, interfaceInfo.getId(), invokeUser.getId());
    }

    private String toInterfaceServicePath(String gatewayPath) {
        if (gatewayPath == null) {
            return null;
        }
        return gatewayPath;
    }

    public Mono<Void> handleResponse(ServerWebExchange exchange,
                                     GatewayFilterChain chain,
                                     long interfaceInfoId,
                                     long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse();
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    if (!(body instanceof Flux)) {
                        return super.writeWith(body);
                    }
                    Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                    return DataBufferUtils.join(fluxBody)
                            .flatMap(dataBuffer -> {
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);

                                HttpStatus statusCode = getStatusCode();
                                if (statusCode == null || statusCode == HttpStatus.OK) {
                                    try {
                                        innerUserInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                    } catch (Exception e) {
                                        log.error("invokeCount error", e);
                                    }
                                }

                                String data = new String(content, StandardCharsets.UTF_8);
                                log.info("响应结果：{}", data);
                                return super.writeWith(Mono.just(bufferFactory.wrap(content)));
                            });
                }
            };
            return chain.filter(exchange.mutate().response(decoratedResponse).build());
        } catch (Exception e) {
            log.error("网关处理响应异常", e);
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    public Mono<Void> handleNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }
}
