// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** getCatalog GET /api/sensorWorkspace/catalog */
export async function getSensorWorkspaceCatalogUsingGET(options?: { [key: string]: any }) {
  return request<any>('/api/sensorWorkspace/catalog', {
    method: 'GET',
    ...(options || {}),
  });
}

/** syncCatalog POST /api/sensorWorkspace/sync */
export async function syncSensorWorkspaceCatalogUsingPOST(options?: { [key: string]: any }) {
  return request<any>('/api/sensorWorkspace/sync', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    ...(options || {}),
  });
}

/** getOverview GET /api/sensorWorkspace/overview */
export async function getSensorWorkspaceOverviewUsingGET(options?: { [key: string]: any }) {
  return request<any>('/api/sensorWorkspace/overview', {
    method: 'GET',
    ...(options || {}),
  });
}

/** getDevices GET /api/sensorWorkspace/devices */
export async function listSensorWorkspaceDevicesUsingGET(options?: { [key: string]: any }) {
  return request<any>('/api/sensorWorkspace/devices', {
    method: 'GET',
    ...(options || {}),
  });
}

/** getRealtimeWorkspace GET /api/sensorWorkspace/realtimeWorkspace */
export async function getSensorWorkspaceRealtimeWorkspaceUsingGET(options?: {
  [key: string]: any;
}) {
  return request<any>('/api/sensorWorkspace/realtimeWorkspace', {
    method: 'GET',
    ...(options || {}),
  });
}

/** createDevice POST /api/sensorWorkspace/devices */
export async function createSensorWorkspaceDeviceUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorWorkspace/devices', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** updateDevice PUT /api/sensorWorkspace/devices/${param0} */
export async function updateSensorWorkspaceDeviceUsingPUT(
  params: {
    deviceId: string;
  },
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  const { deviceId: param0, ...queryParams } = params;
  return request<any>(`/api/sensorWorkspace/devices/${param0}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** deleteDevice DELETE /api/sensorWorkspace/devices/${param0} */
export async function deleteSensorWorkspaceDeviceUsingDELETE(
  params: {
    deviceId: string;
  },
  options?: { [key: string]: any },
) {
  const { deviceId: param0, ...queryParams } = params;
  return request<any>(`/api/sensorWorkspace/devices/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** testDevice POST /api/sensorWorkspace/devices/test */
export async function testSensorWorkspaceDeviceUsingPOST(
  body: Record<string, any>,
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorWorkspace/devices/test', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getRealtimeSnapshot GET /api/sensorWorkspace/realtime */
export async function getSensorWorkspaceRealtimeUsingGET(
  params: {
    limit?: number;
  },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorWorkspace/realtime', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** searchDataset GET /api/sensorWorkspace/search */
export async function searchSensorWorkspaceDatasetUsingGET(
  params: {
    dataset: string;
    deviceToken?: string;
    startDate?: string;
    endDate?: string;
    limit?: number;
  },
  options?: { [key: string]: any },
) {
  return request<any>('/api/sensorWorkspace/search', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}
