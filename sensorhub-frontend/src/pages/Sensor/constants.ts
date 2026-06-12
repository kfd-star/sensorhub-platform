export type SensorDatasetOption = {
  key: string;
  label: string;
  gatewayPath: string;
  statsKey: string;
};

export type SensorDeviceField = {
  name: string;
  type: string;
  unit?: string;
  description: string;
};

export type SensorDevicePreset = {
  type: string;
  label: string;
  icon: string;
  color: string;
  category: string;
  dataEndpoint: string;
  realtimeKey: string;
  datasetKey?: string;
  dataFields: SensorDeviceField[];
};

export const SENSOR_DATASET_OPTIONS: SensorDatasetOption[] = [
  { key: 'gps', label: 'GPS Data', gatewayPath: '/api/sensor/gps-data', statsKey: 'gps' },
  {
    key: 'esp32Weather',
    label: 'ESP32 Weather',
    gatewayPath: '/api/sensor/esp32-weather-data',
    statsKey: 'ESP32_weather',
  },
  {
    key: 'weatherStation1',
    label: 'Weather Station 1',
    gatewayPath: '/api/sensor/weather-station-1-data',
    statsKey: 'weather_station_1',
  },
  {
    key: 'weatherStation2',
    label: 'Weather Station 2',
    gatewayPath: '/api/sensor/weather-station-2-data',
    statsKey: 'weather_station_2',
  },
  { key: 'wind', label: 'Wind Data', gatewayPath: '/api/sensor/wind-data', statsKey: 'wind' },
  { key: 'windOcr', label: 'Wind OCR', gatewayPath: '/api/sensor/wind-ocr-data', statsKey: 'wind_ocr' },
  { key: 'image', label: 'Image Data', gatewayPath: '/api/sensor/image-data', statsKey: 'image' },
];

export const SENSOR_DEVICE_PRESETS: SensorDevicePreset[] = [
  {
    type: 'gps',
    label: '无人机 GPS 设备',
    icon: '[GPS]',
    color: '#1677ff',
    category: '传感器',
    dataEndpoint: '/api/gps-data',
    realtimeKey: 'gps',
    datasetKey: 'gps',
    dataFields: [
      { name: 'gps_position_x', type: 'number', unit: 'deg', description: 'GPS 经度' },
      { name: 'gps_position_y', type: 'number', unit: 'deg', description: 'GPS 纬度' },
      { name: 'gps_position_z', type: 'number', unit: 'm', description: 'GPS 海拔' },
      { name: 'velocity_x', type: 'number', unit: 'm/s', description: '速度 X' },
      { name: 'velocity_y', type: 'number', unit: 'm/s', description: '速度 Y' },
      { name: 'velocity_z', type: 'number', unit: 'm/s', description: '速度 Z' },
      { name: 'pitch', type: 'number', unit: 'deg', description: '俯仰角' },
      { name: 'roll', type: 'number', unit: 'deg', description: '横滚角' },
      { name: 'yaw', type: 'number', unit: 'deg', description: '偏航角' },
    ],
  },
  {
    type: 'ESP32_weather',
    label: 'ESP32 气象站',
    icon: '[WX]',
    color: '#16a34a',
    category: '传感器',
    dataEndpoint: '/api/ESP32-weather-data',
    realtimeKey: 'ESP32_weather',
    datasetKey: 'esp32Weather',
    dataFields: [
      { name: 'temperature', type: 'number', unit: 'C', description: '温度' },
      { name: 'humidity', type: 'number', unit: '%', description: '湿度' },
      { name: 'windSpeed', type: 'number', unit: 'm/s', description: '风速' },
      { name: 'windScale', type: 'number', unit: 'level', description: '风级' },
      { name: 'windDirectionDegree', type: 'number', unit: 'deg', description: '风向角度' },
      { name: 'windDirection', type: 'string', description: '风向' },
      { name: 'rssi', type: 'number', unit: 'dBm', description: '信号强度' },
    ],
  },
  {
    type: 'weather_station_1',
    label: '便携气象站 1',
    icon: '[WS1]',
    color: '#7c3aed',
    category: '传感器',
    dataEndpoint: '/api/weather-station-1-data',
    realtimeKey: 'weather_station_1',
    datasetKey: 'weatherStation1',
    dataFields: [
      { name: 'ambientTemperature', type: 'number', unit: 'C', description: '环境温度' },
      { name: 'ambientHumidity', type: 'number', unit: '%', description: '环境湿度' },
      { name: 'pressure', type: 'number', unit: 'hPa', description: '大气压' },
      { name: 'windSpeed', type: 'number', unit: 'm/s', description: '风速' },
      { name: 'windDirection', type: 'number', unit: 'deg', description: '风向' },
      { name: 'RSSI', type: 'number', unit: 'dBm', description: '信号强度' },
    ],
  },
  {
    type: 'weather_station_2',
    label: '便携气象站 2',
    icon: '[WS2]',
    color: '#9333ea',
    category: '传感器',
    dataEndpoint: '/api/weather-station-2-data',
    realtimeKey: 'weather_station_2',
    datasetKey: 'weatherStation2',
    dataFields: [
      { name: 'ambientTemperature', type: 'number', unit: 'C', description: '环境温度' },
      { name: 'ambientHumidity', type: 'number', unit: '%', description: '环境湿度' },
      { name: 'pressure', type: 'number', unit: 'hPa', description: '大气压' },
      { name: 'windSpeed', type: 'number', unit: 'm/s', description: '风速' },
      { name: 'windDirection', type: 'number', unit: 'deg', description: '风向' },
      { name: 'RSSI', type: 'number', unit: 'dBm', description: '信号强度' },
    ],
  },
  {
    type: 'wind',
    label: '机载风速仪',
    icon: '[WIND]',
    color: '#db2777',
    category: '传感器',
    dataEndpoint: '/api/wind-data',
    realtimeKey: 'wind',
    datasetKey: 'wind',
    dataFields: [
      { name: 'wind_speed', type: 'number', unit: 'm/s', description: '风速' },
      { name: 'windDirectionDegree', type: 'number', unit: 'deg', description: '风向角度' },
      { name: 'windDirection', type: 'string', description: '风向' },
    ],
  },
  {
    type: 'camera',
    label: '图像采集设备',
    icon: '[CAM]',
    color: '#0ea5e9',
    category: '传感器',
    dataEndpoint: '/api/image-data',
    realtimeKey: 'camera',
    datasetKey: 'image',
    dataFields: [
      { name: 'image_name', type: 'string', description: '图片名称' },
      { name: 'image_url', type: 'string', description: '图片地址' },
      { name: 'width', type: 'number', unit: 'px', description: '宽度' },
      { name: 'height', type: 'number', unit: 'px', description: '高度' },
      { name: 'file_size', type: 'number', unit: 'bytes', description: '文件大小' },
    ],
  },
  {
    type: 'wind_ocr',
    label: '风速 OCR 设备',
    icon: '[OCR]',
    color: '#0891b2',
    category: '传感器',
    dataEndpoint: '/api/WindfromM300',
    realtimeKey: 'wind_ocr',
    datasetKey: 'windOcr',
    dataFields: [
      { name: 'timestamp_millisecond', type: 'number', unit: 'ms', description: '时间戳' },
      { name: 'spd', type: 'number', description: '原始 SPD' },
      { name: 'wind_speed', type: 'number', unit: 'm/s', description: '风速' },
      { name: 'wind_direction', type: 'string', description: '风向' },
    ],
  },
  {
    type: 'custom',
    label: '自定义设备',
    icon: '[DEV]',
    color: '#6b7280',
    category: '传感器',
    dataEndpoint: '/api/custom-data',
    realtimeKey: 'custom',
    dataFields: [],
  },
];

export const getSensorDevicePreset = (deviceType?: string) =>
  SENSOR_DEVICE_PRESETS.find((item) => item.type === deviceType) ||
  SENSOR_DEVICE_PRESETS.find((item) => item.type === 'custom');
