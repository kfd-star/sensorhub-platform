package com.sensorhub.platform.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sensorhub.platform.service.SensorCompatibilityService;
import com.sensorhub.platform.service.UserService;
import com.sensorhub.common.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.sensorhub.platform.constant.UserConstant.ADMIN_ROLE;
import static com.sensorhub.platform.constant.UserConstant.USER_LOGIN_STATE;

@Service
@Slf4j
public class SensorCompatibilityServiceImpl implements SensorCompatibilityService {

    private static final String SALT = "sensorhub";

    private static final int EXPORT_LIMIT = 10000;

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

    private static final Map<String, ExportDatasetConfig> EXPORT_DATASET_CONFIG_MAP = new LinkedHashMap<>();

    static {
        registerExportDataset("gps", "sensor_gps_data", "deviceToken", "timestamp");
        registerExportDataset("esp32_weather", "sensor_esp32_weather_data", "device_token", "timestamp");
        registerExportDataset("esp32weather", "sensor_esp32_weather_data", "device_token", "timestamp");
        registerExportDataset("weather_station_1", "sensor_weather_station_1_data", "device_token", "timestamp");
        registerExportDataset("weather_station_2", "sensor_weather_station_2_data", "device_token", "timestamp");
        registerExportDataset("wind", "sensor_wind_data", "device_token", "timestamp");
        registerExportDataset("wind_ocr", "sensor_wind_ocr_data", "device_token", "timestamp");
        registerExportDataset("windfromm300", "sensor_wind_ocr_data", "device_token", "timestamp");
        registerExportDataset("image", "sensor_image_data", "device_token", "timestamp");
    }

    @Resource
    private UserService userService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${sensor.compatibility.auth-token-secret:sensor-system-secret-key-2024}")
    private String authTokenSecret;

    @Value("${sensor.compatibility.auth-token-expire-minutes:30}")
    private long authTokenExpireMinutes;

    private final Gson gson = new Gson();

    @Override
    public ResponseEntity<Map<String, Object>> login(Map<String, Object> payload, HttpServletRequest request) {
        if (payload == null) {
            return badRequest("Validation failed", validationError("body", "Request body must not be empty"));
        }
        String username = firstNonBlank(stringValue(payload, "username"), stringValue(payload, "userAccount"));
        String password = firstNonBlank(stringValue(payload, "password"), stringValue(payload, "userPassword"));
        List<Map<String, Object>> errors = new ArrayList<>();
        if (StringUtils.isBlank(username)) {
            errors.add(validationError("username", "Username must not be empty"));
        }
        if (StringUtils.isBlank(password)) {
            errors.add(validationError("password", "Password must not be empty"));
        }
        if (!errors.isEmpty()) {
            return badRequest("Validation failed", errors);
        }
        User user = userService.getOne(new QueryWrapper<User>()
                .eq("userAccount", username.trim())
                .eq("userPassword", encryptPassword(password.trim())), false);
        if (user == null) {
            return failure(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        request.getSession().setAttribute(USER_LOGIN_STATE, user.getId());
        String token = generateToken(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", toLoginUserMap(user));
        return success("Login successful", data);
    }

    @Override
    public ResponseEntity<Map<String, Object>> verifyToken(HttpServletRequest request) {
        ResolvedUser resolvedUser = resolveUser(request, false);
        if (resolvedUser == null) {
            return failure(HttpStatus.UNAUTHORIZED, "Missing access token");
        }
        if (!resolvedUser.isValid()) {
            return failure(HttpStatus.FORBIDDEN, "Access token is invalid");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", resolvedUser.getTokenView());
        return success("Token is valid", data);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        ResolvedUser resolvedUser = resolveUser(request, false);
        if (resolvedUser == null) {
            return failure(HttpStatus.UNAUTHORIZED, "Missing access token");
        }
        if (!resolvedUser.isValid() || resolvedUser.getUser() == null) {
            return failure(HttpStatus.FORBIDDEN, "Access token is invalid");
        }
        return success(null, toCompatUserMap(resolvedUser.getUser()));
    }

    @Override
    public ResponseEntity<Map<String, Object>> listUsers(HttpServletRequest request) {
        ResolvedUser resolvedUser = resolveUser(request, true);
        if (resolvedUser == null) {
            return failure(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (!resolvedUser.isValid()) {
            return failure(HttpStatus.FORBIDDEN, "Access token is invalid");
        }
        if (!resolvedUser.isAdmin()) {
            return failure(HttpStatus.FORBIDDEN, "Permission denied");
        }
        List<User> userList = userService.list(new QueryWrapper<User>().orderByDesc("createTime"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (User user : userList) {
            data.add(toCompatUserMap(user));
        }
        return success(null, data);
    }

    @Override
    public ResponseEntity<Map<String, Object>> createUser(Map<String, Object> payload, HttpServletRequest request) {
        ResolvedUser resolvedUser = resolveUser(request, true);
        if (resolvedUser == null) {
            return failure(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (!resolvedUser.isValid()) {
            return failure(HttpStatus.FORBIDDEN, "Access token is invalid");
        }
        if (!resolvedUser.isAdmin()) {
            return failure(HttpStatus.FORBIDDEN, "Permission denied");
        }
        if (payload == null) {
            return badRequest("Validation failed", validationError("body", "Request body must not be empty"));
        }
        String username = firstNonBlank(stringValue(payload, "username"), stringValue(payload, "userAccount"));
        String password = firstNonBlank(stringValue(payload, "password"), stringValue(payload, "userPassword"));
        String role = firstNonBlank(stringValue(payload, "role"), stringValue(payload, "userRole"));
        String fullName = firstNonBlank(
                stringValue(payload, "full_name"),
                stringValue(payload, "fullName"),
                stringValue(payload, "userName")
        );
        List<Map<String, Object>> errors = new ArrayList<>();
        if (StringUtils.isBlank(username)) {
            errors.add(validationError("username", "Username must not be empty"));
        }
        if (StringUtils.isBlank(password)) {
            errors.add(validationError("password", "Password must not be empty"));
        } else if (password.trim().length() < 6) {
            errors.add(validationError("password", "Password must be at least 6 characters"));
        }
        if (StringUtils.isBlank(role) || !isSupportedRole(role)) {
            errors.add(validationError("role", "Role is invalid"));
        }
        if (!errors.isEmpty()) {
            return badRequest("Validation failed", errors);
        }
        String userAccount = username.trim();
        long count = userService.count(new QueryWrapper<User>().eq("userAccount", userAccount));
        if (count > 0) {
            return failure(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserName(StringUtils.defaultIfBlank(StringUtils.trimToNull(fullName), userAccount));
        user.setUserRole(normalizeRole(role));
        user.setUserPassword(encryptPassword(password.trim()));
        user.setAccessKey(DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(5)));
        user.setSecretKey(DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(8)));
        boolean saved = userService.save(user);
        if (!saved) {
            return failure(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
        Map<String, Object> response = baseResponse(true, "User created successfully");
        response.put("data", toCompatUserMap(user));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<?> exportData(String dataType,
                                        String format,
                                        String startDate,
                                        String endDate,
                                        String deviceToken) {
        ExportDatasetConfig config = resolveExportDatasetConfig(dataType);
        if (config == null) {
            return failure(HttpStatus.BAD_REQUEST, "Invalid data type");
        }
        List<Map<String, Object>> rows;
        try {
            rows = queryExportRows(config, startDate, endDate, deviceToken);
        } catch (Exception e) {
            log.error("export data failed, type={}", dataType, e);
            Map<String, Object> response = baseResponse(false, "Failed to export data");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        String normalizedFormat = StringUtils.defaultIfBlank(StringUtils.trimToNull(format), "json");
        if ("csv".equalsIgnoreCase(normalizedFormat)) {
            if (rows.isEmpty()) {
                return failure(HttpStatus.NOT_FOUND, "No data found");
            }
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8");
            headers.set(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=" + config.getExportName() + "_data_"
                            + StringUtils.defaultIfBlank(StringUtils.trimToNull(startDate), "all")
                            + "_" + StringUtils.defaultIfBlank(StringUtils.trimToNull(endDate), "all")
                            + ".csv"
            );
            return new ResponseEntity<>("\uFEFF" + buildCsv(rows), headers, HttpStatus.OK);
        }
        Map<String, Object> response = baseResponse(true, null);
        response.put("data", rows);
        response.put("count", rows.size());
        response.put("dataType", config.getExportName());
        response.put("exportTime", isoNow());
        Map<String, Object> dateRange = new LinkedHashMap<>();
        dateRange.put("startDate", StringUtils.isBlank(startDate) ? null : startDate.trim());
        dateRange.put("endDate", StringUtils.isBlank(endDate) ? null : endDate.trim());
        response.put("dateRange", dateRange);
        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> queryExportRows(ExportDatasetConfig config,
                                                      String startDate,
                                                      String endDate,
                                                      String deviceToken) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(config.getTableName())
                .append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (StringUtils.isNotBlank(startDate)) {
            sqlBuilder.append(" AND `").append(config.getTimestampColumn()).append("` >= ?");
            params.add(Timestamp.valueOf(startDate.trim() + " 00:00:00"));
        }
        if (StringUtils.isNotBlank(endDate)) {
            sqlBuilder.append(" AND `").append(config.getTimestampColumn()).append("` <= ?");
            params.add(Timestamp.valueOf(endDate.trim() + " 23:59:59"));
        }
        if (StringUtils.isNotBlank(deviceToken) && StringUtils.isNotBlank(config.getDeviceTokenColumn())) {
            sqlBuilder.append(" AND `").append(config.getDeviceTokenColumn()).append("` = ?");
            params.add(deviceToken.trim());
        }
        sqlBuilder.append(" ORDER BY `").append(config.getTimestampColumn()).append("` DESC LIMIT ").append(EXPORT_LIMIT);
        return jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
    }

    private ResolvedUser resolveUser(HttpServletRequest request, boolean allowSessionFallback) {
        String token = extractToken(request);
        if (StringUtils.isNotBlank(token)) {
            return resolveUserFromToken(token.trim());
        }
        if (!allowSessionFallback) {
            return resolveUserFromSession(request);
        }
        ResolvedUser sessionUser = resolveUserFromSession(request);
        return sessionUser != null ? sessionUser : null;
    }

    private ResolvedUser resolveUserFromSession(HttpServletRequest request) {
        try {
            User user = userService.getLoginUser(request);
            Map<String, Object> tokenView = buildTokenView(user, null);
            return new ResolvedUser(user, tokenView, true);
        } catch (Exception e) {
            return null;
        }
    }

    private ResolvedUser resolveUserFromToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return ResolvedUser.invalid();
        }
        String payloadPart = parts[0];
        String signaturePart = parts[1];
        String expectedSignature = hmacSha256Hex(payloadPart, authTokenSecret);
        if (!MessageDigest.isEqual(signaturePart.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            return ResolvedUser.invalid();
        }
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);
            Map<String, Object> payload = gson.fromJson(payloadJson, MAP_TYPE);
            long userId = toLong(payload.get("id"));
            long expireAt = toLong(payload.get("exp"));
            if (userId <= 0 || expireAt <= 0 || expireAt < System.currentTimeMillis()) {
                return ResolvedUser.invalid();
            }
            User user = userService.getById(userId);
            if (user == null) {
                return ResolvedUser.invalid();
            }
            Map<String, Object> tokenView = buildTokenView(user, expireAt);
            return new ResolvedUser(user, tokenView, true);
        } catch (Exception e) {
            return ResolvedUser.invalid();
        }
    }

    private String generateToken(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.getId());
        payload.put("username", user.getUserAccount());
        payload.put("role", normalizeRole(user.getUserRole()));
        payload.put("exp", System.currentTimeMillis() + authTokenExpireMinutes * 60 * 1000);
        payload.put("iat", System.currentTimeMillis());
        String payloadJson = gson.toJson(payload);
        String payloadPart = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return payloadPart + "." + hmacSha256Hex(payloadPart, authTokenSecret);
    }

    private String hmacSha256Hex(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("failed to sign auth token", e);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(authorization)) {
            return null;
        }
        String prefix = "Bearer ";
        if (authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return authorization.substring(prefix.length()).trim();
        }
        return authorization.trim();
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes(StandardCharsets.UTF_8));
    }

    private String buildCsv(List<Map<String, Object>> rows) {
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        List<String> lines = new ArrayList<>();
        lines.add(String.join(",", headers));
        for (Map<String, Object> row : rows) {
            List<String> fields = new ArrayList<>();
            for (String header : headers) {
                fields.add(toCsvField(row.get(header)));
            }
            lines.add(String.join(",", fields));
        }
        return String.join("\n", lines);
    }

    private String toCsvField(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        if (text.contains("\"")) {
            text = text.replace("\"", "\"\"");
        }
        return needQuote ? "\"" + text + "\"" : text;
    }

    private Map<String, Object> toLoginUserMap(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUserAccount());
        result.put("role", normalizeRole(user.getUserRole()));
        result.put("full_name", StringUtils.trimToNull(user.getUserName()));
        result.put("email", null);
        return result;
    }

    private Map<String, Object> toCompatUserMap(User user) {
        Map<String, Object> result = toLoginUserMap(user);
        result.put("is_active", true);
        result.put("last_login", null);
        result.put("created_at", formatDate(user.getCreateTime()));
        return result;
    }

    private Map<String, Object> buildTokenView(User user, Long expireAt) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUserAccount());
        result.put("role", normalizeRole(user.getUserRole()));
        if (expireAt != null) {
            result.put("exp", expireAt);
        }
        return result;
    }

    private String formatDate(Date value) {
        if (value == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(value);
    }

    private String isoNow() {
        return formatDate(new Date());
    }

    private boolean isSupportedRole(String role) {
        String normalized = normalizeRole(role);
        return StringUtils.equalsAny(normalized, ADMIN_ROLE, "operator", "viewer", "user");
    }

    private String normalizeRole(String role) {
        if (StringUtils.isBlank(role)) {
            return "user";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if ("sensor_admin".equals(normalized)) {
            return ADMIN_ROLE;
        }
        return normalized;
    }

    private ExportDatasetConfig resolveExportDatasetConfig(String dataType) {
        if (StringUtils.isBlank(dataType)) {
            return null;
        }
        return EXPORT_DATASET_CONFIG_MAP.get(dataType.trim().toLowerCase(Locale.ROOT));
    }

    private static void registerExportDataset(String name,
                                              String tableName,
                                              String deviceTokenColumn,
                                              String timestampColumn) {
        EXPORT_DATASET_CONFIG_MAP.put(name, new ExportDatasetConfig(name, tableName, deviceTokenColumn, timestampColumn));
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return -1L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return -1L;
        }
    }

    private String stringValue(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key) || payload.get(key) == null) {
            return null;
        }
        return String.valueOf(payload.get(key));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> success(String message, Object data) {
        Map<String, Object> response = baseResponse(true, message);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message, Map<String, Object> error) {
        List<Map<String, Object>> errors = new ArrayList<>();
        errors.add(error);
        return badRequest(message, errors);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message, List<Map<String, Object>> errors) {
        Map<String, Object> response = baseResponse(false, message);
        response.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private ResponseEntity<Map<String, Object>> failure(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(baseResponse(false, message));
    }

    private Map<String, Object> baseResponse(boolean success, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        if (StringUtils.isNotBlank(message)) {
            response.put("message", message);
        }
        return response;
    }

    private Map<String, Object> validationError(String field, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("field", field);
        error.put("msg", message);
        return error;
    }

    private static class ExportDatasetConfig {
        private final String exportName;
        private final String tableName;
        private final String deviceTokenColumn;
        private final String timestampColumn;

        private ExportDatasetConfig(String exportName, String tableName, String deviceTokenColumn, String timestampColumn) {
            this.exportName = exportName;
            this.tableName = tableName;
            this.deviceTokenColumn = deviceTokenColumn;
            this.timestampColumn = timestampColumn;
        }

        private String getExportName() {
            return exportName;
        }

        private String getTableName() {
            return tableName;
        }

        private String getDeviceTokenColumn() {
            return deviceTokenColumn;
        }

        private String getTimestampColumn() {
            return timestampColumn;
        }
    }

    private static class ResolvedUser {
        private final User user;
        private final Map<String, Object> tokenView;
        private final boolean valid;

        private ResolvedUser(User user, Map<String, Object> tokenView, boolean valid) {
            this.user = user;
            this.tokenView = tokenView;
            this.valid = valid;
        }

        private static ResolvedUser invalid() {
            return new ResolvedUser(null, null, false);
        }

        private User getUser() {
            return user;
        }

        private Map<String, Object> getTokenView() {
            return tokenView;
        }

        private boolean isValid() {
            return valid;
        }

        private boolean isAdmin() {
            return user != null && ADMIN_ROLE.equalsIgnoreCase(user.getUserRole());
        }
    }
}
