package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sensorhub.platform.model.entity.SensorDevice;
import com.sensorhub.platform.model.entity.SensorRealtimeChannel;
import com.sensorhub.platform.model.vo.SensorRealtimeMessageVO;
import com.sensorhub.platform.service.SensorDeviceService;
import com.sensorhub.platform.service.SensorNorthboundService;
import com.sensorhub.platform.service.SensorRealtimeChannelService;
import com.sensorhub.platform.service.SensorRealtimeStreamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SensorRealtimeStreamServiceImpl implements SensorRealtimeStreamService {

    @Resource
    private SensorRealtimeChannelService sensorRealtimeChannelService;

    @Resource
    private SensorDeviceService sensorDeviceService;

    @Resource
    private SensorNorthboundService sensorNorthboundService;

    @Override
    public List<SensorRealtimeMessageVO> listLatestMessages() {
        List<SensorRealtimeChannel> channelList = sensorRealtimeChannelService.list(
                new QueryWrapper<SensorRealtimeChannel>().eq("status", 1).orderByAsc("id")
        );
        if (channelList.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, SensorDevice> deviceByRealtimeKey = sensorDeviceService.list(
                new QueryWrapper<SensorDevice>().eq("status", 1).orderByAsc("id")
        ).stream()
                .filter(device -> StringUtils.isNotBlank(device.getRealtimeKey()))
                .collect(Collectors.toMap(
                        SensorDevice::getRealtimeKey,
                        device -> device,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        List<SensorRealtimeMessageVO> messages = new ArrayList<SensorRealtimeMessageVO>();
        for (SensorRealtimeChannel channel : channelList) {
            SensorDevice device = resolveDevice(channel, deviceByRealtimeKey);
            if (device == null) {
                continue;
            }
            Map<String, Object> latestRecord = loadLatestRecord(device, channel);
            if (latestRecord.isEmpty()) {
                continue;
            }
            SensorRealtimeMessageVO message = new SensorRealtimeMessageVO();
            message.setType("telemetry");
            message.setDevice(StringUtils.defaultIfBlank(channel.getTopic(), device.getRealtimeKey()));
            message.setDeviceName(StringUtils.defaultIfBlank(device.getDeviceName(), channel.getName()));
            message.setTimestamp(extractTimestamp(latestRecord));
            message.setData(latestRecord);
            messages.add(message);
        }
        return messages;
    }

    private SensorDevice resolveDevice(SensorRealtimeChannel channel, Map<String, SensorDevice> deviceByRealtimeKey) {
        if (channel == null) {
            return null;
        }
        if (StringUtils.isNotBlank(channel.getTopic())) {
            SensorDevice device = deviceByRealtimeKey.get(channel.getTopic());
            if (device != null) {
                return device;
            }
        }
        return sensorDeviceService.getOne(
                new QueryWrapper<SensorDevice>()
                        .eq(StringUtils.isNotBlank(channel.getDeviceType()), "deviceType", channel.getDeviceType())
                        .eq("status", 1)
                        .orderByAsc("id")
                        .last("LIMIT 1"),
                false
        );
    }

    private Map<String, Object> loadLatestRecord(SensorDevice device, SensorRealtimeChannel channel) {
        String deviceToken = StringUtils.trimToNull(device.getDeviceToken());
        String normalizedEndpoint = normalizeEndpoint(device.getDataEndpoint());
        try {
            if (matchesDataset(normalizedEndpoint, device.getDeviceType(), channel.getTopic(), "gps-data", "gps")) {
                return extractFirstRecord(sensorNorthboundService.listGpsData(1, 0, deviceToken, null, null));
            }
            if (matchesDataset(normalizedEndpoint, device.getDeviceType(), channel.getTopic(),
                    "esp32-weather-data", "esp32Weather", "esp32_weather")) {
                return extractFirstRecord(sensorNorthboundService.listEsp32WeatherData(1, 0, deviceToken, null, null));
            }
            if (matchesDataset(normalizedEndpoint, device.getDeviceType(), channel.getTopic(),
                    "weather-station-1-data", "weather_station_1", "weatherStation1")) {
                return extractFirstRecord(sensorNorthboundService.listWeatherStation1Data(1, 0, deviceToken, null, null));
            }
            if (matchesDataset(normalizedEndpoint, device.getDeviceType(), channel.getTopic(),
                    "weather-station-2-data", "weather_station_2", "weatherStation2")) {
                return extractFirstRecord(sensorNorthboundService.listWeatherStation2Data(1, 0, deviceToken, null, null));
            }
            if (matchesDataset(normalizedEndpoint, device.getDeviceType(), channel.getTopic(),
                    "wind-data", "wind")) {
                return extractFirstRecord(sensorNorthboundService.listWindData(1, 0, deviceToken, null, null));
            }
            if (matchesDataset(normalizedEndpoint, device.getDeviceType(), channel.getTopic(),
                    "windfromm300", "wind-ocr-data", "wind_ocr", "windOcr")) {
                return extractFirstRecord(sensorNorthboundService.listWindOcrData(1, 0, deviceToken, null, null));
            }
            if (matchesDataset(normalizedEndpoint, device.getDeviceType(), channel.getTopic(),
                    "image-data", "camera", "image")) {
                return extractFirstRecord(sensorNorthboundService.listImageData(1, 0, deviceToken, null, null));
            }
        } catch (Exception e) {
            log.warn("load realtime record failed, deviceCode={}, reason={}", device.getDeviceCode(), e.getMessage());
        }
        return Collections.emptyMap();
    }

    private boolean matchesDataset(String normalizedEndpoint, String deviceType, String topic, String endpointKeyword, String datasetCode) {
        return StringUtils.containsIgnoreCase(normalizedEndpoint, endpointKeyword)
                || StringUtils.equalsAnyIgnoreCase(StringUtils.defaultString(deviceType), datasetCode)
                || StringUtils.equalsAnyIgnoreCase(StringUtils.defaultString(topic), datasetCode);
    }

    private boolean matchesDataset(String normalizedEndpoint,
                                   String deviceType,
                                   String topic,
                                   String endpointKeyword,
                                   String datasetCode,
                                   String alternateCode) {
        return matchesDataset(normalizedEndpoint, deviceType, topic, endpointKeyword, datasetCode)
                || StringUtils.equalsAnyIgnoreCase(StringUtils.defaultString(deviceType), alternateCode)
                || StringUtils.equalsAnyIgnoreCase(StringUtils.defaultString(topic), alternateCode);
    }

    private boolean matchesDataset(String normalizedEndpoint,
                                   String deviceType,
                                   String topic,
                                   String endpointKeyword,
                                   String datasetCode,
                                   String alternateCode,
                                   String secondAlternateCode) {
        return matchesDataset(normalizedEndpoint, deviceType, topic, endpointKeyword, datasetCode, alternateCode)
                || StringUtils.equalsAnyIgnoreCase(StringUtils.defaultString(deviceType), secondAlternateCode)
                || StringUtils.equalsAnyIgnoreCase(StringUtils.defaultString(topic), secondAlternateCode);
    }

    private String normalizeEndpoint(String endpoint) {
        if (StringUtils.isBlank(endpoint)) {
            return "";
        }
        return endpoint.trim().replace("/api/", "").replace("/", "").toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFirstRecord(Map<String, Object> response) {
        if (response == null) {
            return Collections.emptyMap();
        }
        Object data = response.get("data");
        if (!(data instanceof List)) {
            return Collections.emptyMap();
        }
        List<?> records = (List<?>) data;
        if (records.isEmpty()) {
            return Collections.emptyMap();
        }
        Object firstRecord = records.get(0);
        if (firstRecord instanceof Map) {
            return (Map<String, Object>) firstRecord;
        }
        return Collections.emptyMap();
    }

    private String extractTimestamp(Map<String, Object> record) {
        if (record == null || record.isEmpty()) {
            return null;
        }
        Object timestamp = record.get("timestamp");
        if (timestamp != null) {
            return String.valueOf(timestamp);
        }
        Object timestampMillisecond = record.get("timestamp_millisecond");
        if (timestampMillisecond == null) {
            timestampMillisecond = record.get("timestampMillisecond");
        }
        return timestampMillisecond == null ? null : String.valueOf(timestampMillisecond);
    }
}
