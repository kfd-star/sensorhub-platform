import React from 'react';
import { Image, Tag, Typography } from 'antd';
import { getSensorDevicePreset } from './constants';
import DataItem from './components/DataItem';
import WindDirectionIndicator from './components/WindDirectionIndicator';

export type SensorDeviceRecord = Record<string, any>;

export const safeNumber = (value: any): number | null => {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
};

export const safeToFixed = (value: any, digits = 2): string => {
  const numberValue = safeNumber(value);
  return numberValue === null ? '--' : numberValue.toFixed(digits);
};

export const getDeviceRealtimeKey = (device: SensorDeviceRecord) => {
  return (
    device.realtime_key ||
    device.realtimeKey ||
    getSensorDevicePreset(device.type || device.deviceType)?.realtimeKey ||
    device.type ||
    device.deviceType ||
    device.id
  );
};

export const getDeviceStatus = (timestamp?: string) => {
  if (!timestamp) {
    return 'offline' as const;
  }
  const time = new Date(timestamp).getTime();
  if (!Number.isFinite(time)) {
    return 'offline' as const;
  }
  return Date.now() - time < 120000 ? ('online' as const) : ('offline' as const);
};

const renderWindPanel = (directionValue: any, speedValue: any) => {
  const direction = safeNumber(directionValue);
  const speed = safeNumber(speedValue);
  if (direction === null || speed === null) {
    return null;
  }
  return <WindDirectionIndicator direction={direction} speed={speed} />;
};

export const renderRealtimeMetrics = (device: SensorDeviceRecord, data?: Record<string, any>) => {
  if (!data) {
    return (
      <Typography.Text type="secondary">
        暂无实时数据，设备接入后这里会显示最新监测值。
      </Typography.Text>
    );
  }

  const deviceType = device.type || device.deviceType;

  if (deviceType === 'gps') {
    return (
      <>
        <DataItem label="GPS 经度" value={safeToFixed(data.gps_position_x, 6)} unit="deg" color="#1677ff" />
        <DataItem label="GPS 纬度" value={safeToFixed(data.gps_position_y, 6)} unit="deg" color="#1677ff" />
        <DataItem label="GPS 海拔" value={safeToFixed(data.gps_position_z)} unit="m" color="#1677ff" />
        <DataItem label="速度 X" value={safeToFixed(data.velocity_x, 3)} unit="m/s" color="#1677ff" />
        <DataItem label="速度 Y" value={safeToFixed(data.velocity_y, 3)} unit="m/s" color="#1677ff" />
        <DataItem label="速度 Z" value={safeToFixed(data.velocity_z, 3)} unit="m/s" color="#1677ff" />
        <DataItem label="俯仰角" value={safeToFixed(data.pitch, 2)} unit="deg" color="#1677ff" />
        <DataItem label="横滚角" value={safeToFixed(data.roll, 2)} unit="deg" color="#1677ff" />
        <DataItem label="偏航角" value={safeToFixed(data.yaw, 2)} unit="deg" color="#1677ff" />
      </>
    );
  }

  if (deviceType === 'ESP32_weather') {
    return (
      <>
        <DataItem label="温度" value={safeToFixed(data.temperature)} unit="C" color="#16a34a" />
        <DataItem label="湿度" value={safeToFixed(data.humidity)} unit="%" color="#16a34a" />
        <DataItem label="风速" value={safeToFixed(data.windSpeed || data.windspeed)} unit="m/s" color="#16a34a" />
        <DataItem label="风级" value={safeToFixed(data.windScale, 0)} unit="level" color="#16a34a" />
        <DataItem label="风向" value={data.windDirection || '--'} color="#16a34a" />
        <DataItem label="信号强度" value={safeToFixed(data.rssi, 0)} unit="dBm" color="#16a34a" />
        {renderWindPanel(data.windDirectionDegree || data.winddirectiondegree, data.windSpeed || data.windspeed)}
      </>
    );
  }

  if (deviceType === 'weather_station_1' || deviceType === 'weather_station_2') {
    const color = deviceType === 'weather_station_1' ? '#7c3aed' : '#9333ea';
    return (
      <>
        <DataItem label="环境温度" value={safeToFixed(data.ambientTemperature)} unit="C" color={color} />
        <DataItem label="环境湿度" value={safeToFixed(data.ambientHumidity)} unit="%" color={color} />
        <DataItem label="大气压" value={safeToFixed(data.pressure)} unit="hPa" color={color} />
        <DataItem label="风速" value={safeToFixed(data.windSpeed)} unit="m/s" color={color} />
        <DataItem label="风向" value={safeToFixed(data.windDirection, 0)} unit="deg" color={color} />
        <DataItem label="信号强度" value={safeToFixed(data.RSSI, 0)} unit="dBm" color={color} />
        {renderWindPanel(data.windDirection, data.windSpeed)}
      </>
    );
  }

  if (deviceType === 'wind') {
    return (
      <>
        <DataItem label="风速" value={safeToFixed(data.windSpeed || data.wind_speed)} unit="m/s" color="#db2777" />
        <DataItem
          label="风向角度"
          value={safeToFixed(data.windDirectionDegree || data.winddirectiondegree, 0)}
          unit="deg"
          color="#db2777"
        />
        <DataItem label="风向" value={data.windDirection || '--'} color="#db2777" />
        {renderWindPanel(data.windDirectionDegree || data.winddirectiondegree, data.windSpeed || data.wind_speed)}
      </>
    );
  }

  if (deviceType === 'wind_ocr') {
    return (
      <>
        <DataItem label="时间戳" value={safeToFixed(data.timestamp_millisecond, 0)} unit="ms" color="#0891b2" />
        <DataItem label="SPD 原始值" value={safeToFixed(data.spd, 3)} color="#0891b2" />
        <DataItem label="风速" value={safeToFixed(data.wind_speed)} unit="m/s" color="#0891b2" />
        <DataItem label="风向" value={data.wind_direction || '--'} color="#0891b2" />
        {renderWindPanel(data.wind_direction_degree, data.wind_speed)}
      </>
    );
  }

  if (deviceType === 'camera') {
    return (
      <>
        <DataItem label="图片名称" value={data.image_name || '--'} color="#0ea5e9" />
        <DataItem
          label="分辨率"
          value={data.width && data.height ? `${data.width} x ${data.height}` : '--'}
          color="#0ea5e9"
        />
        <DataItem
          label="文件大小"
          value={
            safeNumber(data.file_size) === null
              ? '--'
              : `${Math.round((safeNumber(data.file_size) || 0) / 1024)} KB`
          }
          color="#0ea5e9"
        />
        {data.image_url ? (
          <div style={{ marginTop: 16 }}>
            <Image
              src={data.image_url}
              alt={data.image_name || 'camera'}
              style={{ width: '100%', borderRadius: 12, maxHeight: 180, objectFit: 'cover' }}
            />
          </div>
        ) : null}
      </>
    );
  }

  const dataFields = Array.isArray(device.data_fields) ? device.data_fields : [];
  if (dataFields.length > 0) {
    return (
      <>
        {dataFields.slice(0, 8).map((field: Record<string, any>) => (
          <DataItem
            key={field.name}
            label={field.description || field.label || field.name}
            value={field.type === 'number' ? safeToFixed(data[field.name]) : (data[field.name] ?? '--')}
            unit={field.unit}
            color="#6b7280"
          />
        ))}
      </>
    );
  }

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {Object.entries(data)
        .slice(0, 8)
        .map(([key, value]) => (
          <Tag key={key} color="blue">
            {`${key}: ${typeof value === 'object' ? JSON.stringify(value) : String(value)}`}
          </Tag>
        ))}
    </div>
  );
};
