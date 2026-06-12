// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** getDashboard GET /api/sensorRegistry/dashboard */
export async function getSensorRegistryDashboardUsingGET(options?: { [key: string]: any }) {
  return request<any>('/api/sensorRegistry/dashboard', {
    method: 'GET',
    ...(options || {}),
  });
}

/** bootstrapRegistry POST /api/sensorRegistry/bootstrap */
export async function bootstrapSensorRegistryUsingPOST(options?: { [key: string]: any }) {
  return request<any>('/api/sensorRegistry/bootstrap', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    ...(options || {}),
  });
}

/** syncDevices POST /api/sensorRegistry/sync/devices */
export async function syncSensorRegistryDevicesUsingPOST(options?: { [key: string]: any }) {
  return request<any>('/api/sensorRegistry/sync/devices', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    ...(options || {}),
  });
}

/** syncEndpoints POST /api/sensorRegistry/sync/endpoints */
export async function syncSensorRegistryEndpointsUsingPOST(options?: { [key: string]: any }) {
  return request<any>('/api/sensorRegistry/sync/endpoints', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    ...(options || {}),
  });
}

/** syncBindings POST /api/sensorRegistry/sync/bindings */
export async function syncSensorRegistryBindingsUsingPOST(options?: { [key: string]: any }) {
  return request<any>('/api/sensorRegistry/sync/bindings', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    ...(options || {}),
  });
}

/** syncRealtimeChannels POST /api/sensorRegistry/sync/realtimeChannels */
export async function syncSensorRegistryRealtimeChannelsUsingPOST(options?: {
  [key: string]: any;
}) {
  return request<any>('/api/sensorRegistry/sync/realtimeChannels', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    ...(options || {}),
  });
}

/** listDataSources GET /api/sensorRegistry/dataSource/list */
export async function listSensorRegistryDataSourcesUsingGET(options?: { [key: string]: any }) {
  return request<any>('/api/sensorRegistry/dataSource/list', {
    method: 'GET',
    ...(options || {}),
  });
}

/** listDevicesByPage GET /api/sensorRegistry/device/list/page */
export async function listSensorRegistryDevicesByPageUsingGET(
  params: {
    current?: number;
    pageSize?: number;
    deviceCode?: string;
    deviceName?: string;
    deviceType?: string;
    dataSourceId?: number;
    status?: number;
  },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/device/list/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** listPublicDevices GET /api/sensorRegistry/device/public/list */
export async function listPublicSensorRegistryDevicesUsingGET(
  params?: {
    deviceCode?: string;
    deviceName?: string;
    deviceType?: string;
    dataSourceId?: number;
    status?: number;
  },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/device/public/list', {
    method: 'GET',
    params: {
      ...(params || {}),
    },
    ...(options || {}),
  });
}

/** listApiEndpointsByPage GET /api/sensorRegistry/apiEndpoint/list/page */
export async function listSensorRegistryApiEndpointsByPageUsingGET(
  params: {
    current?: number;
    pageSize?: number;
    endpointCode?: string;
    name?: string;
    method?: string;
    targetService?: string;
    category?: string;
    status?: number;
  },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/apiEndpoint/list/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** listBindingsByPage GET /api/sensorRegistry/binding/list/page */
export async function listSensorRegistryBindingsByPageUsingGET(
  params: {
    current?: number;
    pageSize?: number;
    sensorApiEndpointId?: number;
    interfaceInfoId?: number;
    status?: number;
  },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/binding/list/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** listRealtimeChannelsByPage GET /api/sensorRegistry/realtimeChannel/list/page */
export async function listSensorRegistryRealtimeChannelsByPageUsingGET(
  params: {
    current?: number;
    pageSize?: number;
    channelCode?: string;
    name?: string;
    deviceType?: string;
    authType?: string;
    status?: number;
  },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/realtimeChannel/list/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** addSensorDevice POST /api/sensorRegistry/device/add */
export async function addSensorRegistryDeviceUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/device/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** updateSensorDevice POST /api/sensorRegistry/device/update */
export async function updateSensorRegistryDeviceUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/device/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteSensorDevice POST /api/sensorRegistry/device/delete */
export async function deleteSensorRegistryDeviceUsingPOST(
  body: { id: number },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/device/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** addSensorApiEndpoint POST /api/sensorRegistry/apiEndpoint/add */
export async function addSensorRegistryApiEndpointUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/apiEndpoint/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** updateSensorApiEndpoint POST /api/sensorRegistry/apiEndpoint/update */
export async function updateSensorRegistryApiEndpointUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/apiEndpoint/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteSensorApiEndpoint POST /api/sensorRegistry/apiEndpoint/delete */
export async function deleteSensorRegistryApiEndpointUsingPOST(
  body: { id: number },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/apiEndpoint/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** addSensorInterfaceBinding POST /api/sensorRegistry/binding/add */
export async function addSensorRegistryBindingUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/binding/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** updateSensorInterfaceBinding POST /api/sensorRegistry/binding/update */
export async function updateSensorRegistryBindingUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/binding/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteSensorInterfaceBinding POST /api/sensorRegistry/binding/delete */
export async function deleteSensorRegistryBindingUsingPOST(
  body: { id: number },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/binding/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** addSensorRealtimeChannel POST /api/sensorRegistry/realtimeChannel/add */
export async function addSensorRegistryRealtimeChannelUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/realtimeChannel/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** updateSensorRealtimeChannel POST /api/sensorRegistry/realtimeChannel/update */
export async function updateSensorRegistryRealtimeChannelUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/realtimeChannel/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteSensorRealtimeChannel POST /api/sensorRegistry/realtimeChannel/delete */
export async function deleteSensorRegistryRealtimeChannelUsingPOST(
  body: { id: number },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorRegistry/realtimeChannel/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}
