import { PageContainer } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import React, { useEffect, useState } from 'react';
import {
  addInterfaceInfoUsingPOST,
  listInterfaceInfoByPageUsingGET,
  onlineInterfaceInfoUsingPOST,
} from '@/services/sensorhub-backend/interfaceInfoController';
import {
  getSensorDevicePreset,
  SENSOR_DATASET_OPTIONS,
} from '@/pages/Sensor/constants';
import {
  addSensorRegistryApiEndpointUsingPOST,
  addSensorRegistryBindingUsingPOST,
  addSensorRegistryDeviceUsingPOST,
  addSensorRegistryRealtimeChannelUsingPOST,
  bootstrapSensorRegistryUsingPOST,
  deleteSensorRegistryApiEndpointUsingPOST,
  deleteSensorRegistryBindingUsingPOST,
  deleteSensorRegistryDeviceUsingPOST,
  deleteSensorRegistryRealtimeChannelUsingPOST,
  getSensorRegistryDashboardUsingGET,
  listSensorRegistryApiEndpointsByPageUsingGET,
  listSensorRegistryBindingsByPageUsingGET,
  listSensorRegistryDataSourcesUsingGET,
  listSensorRegistryDevicesByPageUsingGET,
  listSensorRegistryRealtimeChannelsByPageUsingGET,
  syncSensorRegistryBindingsUsingPOST,
  syncSensorRegistryDevicesUsingPOST,
  syncSensorRegistryEndpointsUsingPOST,
  syncSensorRegistryRealtimeChannelsUsingPOST,
  updateSensorRegistryApiEndpointUsingPOST,
  updateSensorRegistryBindingUsingPOST,
  updateSensorRegistryDeviceUsingPOST,
  updateSensorRegistryRealtimeChannelUsingPOST,
} from '@/services/sensorhub-backend/sensorRegistryController';

type TabKey = 'dataSources' | 'devices' | 'endpoints' | 'bindings' | 'realtimeChannels';

type PageData<T> = {
  records?: T[];
  total?: number;
  current?: number;
  size?: number;
};

type DashboardData = {
  dataSourceCount?: number;
  deviceCount?: number;
  endpointCount?: number;
  bindingCount?: number;
  realtimeChannelCount?: number;
  publishedInterfaceCount?: number;
  defaultDataSourceCode?: string;
};

type SyncResult = {
  total?: number;
  createdCount?: number;
  updatedCount?: number;
  skippedCount?: number;
};

type DataSourceItem = {
  id?: number;
  code?: string;
  name?: string;
  type?: string;
  baseUrl?: string;
  wsUrl?: string;
  status?: number;
};

type DeviceItem = {
  id?: number;
  deviceCode?: string;
  deviceName?: string;
  deviceType?: string;
  category?: string;
  description?: string;
  deviceToken?: string;
  dataSourceId?: number;
  legacyDeviceId?: string;
  dataEndpoint?: string;
  realtimeKey?: string;
  configJson?: string;
  dataFieldsJson?: string;
  status?: number;
};

type EndpointItem = {
  id?: number;
  endpointCode?: string;
  name?: string;
  description?: string;
  method?: string;
  internalPath?: string;
  targetPath?: string;
  targetService?: string;
  category?: string;
  protocolType?: string;
  requestSchema?: string;
  responseSchema?: string;
  status?: number;
};

type BindingItem = {
  id?: number;
  sensorApiEndpointId?: number;
  interfaceInfoId?: number;
  endpointName?: string;
  endpointCode?: string;
  internalPath?: string;
  method?: string;
  interfaceName?: string;
  interfaceUrl?: string;
  interfaceStatus?: number;
  publishStrategy?: string;
  authStrategy?: string;
  limitStrategy?: string;
  cacheStrategy?: string;
  status?: number;
};

type RealtimeChannelItem = {
  id?: number;
  channelCode?: string;
  name?: string;
  wsPath?: string;
  topic?: string;
  deviceType?: string;
  authType?: string;
  status?: number;
};

type OptionItem = {
  label: string;
  value: number;
};

type DeviceFilter = {
  deviceCode?: string;
  deviceName?: string;
  deviceType?: string;
  dataSourceId?: number;
  status?: number;
};

type EndpointFilter = {
  endpointCode?: string;
  name?: string;
  method?: string;
  targetService?: string;
  category?: string;
  status?: number;
};

type BindingFilter = {
  sensorApiEndpointId?: number;
  interfaceInfoId?: number;
  status?: number;
};

type ChannelFilter = {
  channelCode?: string;
  name?: string;
  deviceType?: string;
  authType?: string;
  status?: number;
};

const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const METHOD_OPTIONS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map((value) => ({
  label: value,
  value,
}));

const emptyPage = { records: [], total: 0, current: 1, size: 10 };

const emptyTabLoadingState: Record<TabKey, boolean> = {
  dataSources: false,
  devices: false,
  endpoints: false,
  bindings: false,
  realtimeChannels: false,
};

const renderStatusTag = (status?: number, onText = '启用', offText = '停用') =>
  status === 1 ? <Tag color="green">{onText}</Tag> : <Tag>{offText}</Tag>;

const cleanQuery = <T extends Record<string, any>>(query: T): Partial<T> =>
  Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  ) as Partial<T>;

const buildPageFallback = <T,>(current: number, pageSize: number): PageData<T> => ({
  ...emptyPage,
  current,
  size: pageSize,
});

type PublishAssistantCatalog = {
  endpoints: EndpointItem[];
  interfaces: API.InterfaceInfo[];
  bindings: BindingItem[];
};

type PublishAssistantSuggestion = {
  presetLabel?: string;
  datasetLabel?: string;
  datasetKey?: string;
  gatewayPath?: string;
  targetPath?: string;
  endpoint?: EndpointItem;
  interfaceInfo?: API.InterfaceInfo;
  interfaceOnline?: boolean;
  binding?: BindingItem;
  endpointDraft?: Record<string, any>;
  interfaceDraft?: API.InterfaceInfoAddRequest;
  bindingDraft?: Record<string, any>;
  issues: string[];
  notices: string[];
};

const DEFAULT_INTERFACE_REQUEST_HEADERS = JSON.stringify(
  [
    {
      name: 'accessKey',
      required: true,
      description: 'platform access key',
    },
    {
      name: 'nonce',
      required: true,
      description: 'request nonce',
    },
    {
      name: 'timestamp',
      required: true,
      description: 'unix timestamp',
    },
    {
      name: 'sign',
      required: true,
      description: 'gateway signature',
    },
    {
      name: 'body',
      required: true,
      description: 'request body used in signing',
    },
  ],
  null,
  2,
);

const DEFAULT_INTERFACE_RESPONSE_HEADERS = JSON.stringify(
  [{ name: 'Content-Type', value: 'application/json' }],
  null,
  2,
);

const normalizePath = (value?: string) => {
  if (!value) {
    return undefined;
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  const normalized = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  return normalized.replace(/\/{2,}/g, '/');
};

const normalizeMethod = (value?: string) => value?.trim().toUpperCase() || undefined;

const sanitizeCodeFragment = (value?: string) => {
  const normalized = (value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
  return normalized || 'sensor_device';
};

const buildDefaultGatewayPath = (targetPath?: string) => {
  const normalizedTargetPath = normalizePath(targetPath);
  if (!normalizedTargetPath) {
    return undefined;
  }
  if (normalizedTargetPath.startsWith('/api/sensor/')) {
    return normalizedTargetPath;
  }
  if (normalizedTargetPath.startsWith('/api/')) {
    return normalizePath(`/api/sensor/${normalizedTargetPath.slice('/api/'.length)}`);
  }
  return normalizePath(`/api/sensor${normalizedTargetPath}`);
};

const buildInterfaceRequestParams = (device: DeviceItem, datasetKey?: string) =>
  JSON.stringify(
    [
      {
        name: 'device_token',
        type: 'string',
        required: false,
        description: `device token filter${datasetKey ? ` for ${datasetKey}` : ''}`,
      },
      {
        name: 'startDate',
        type: 'string',
        required: false,
        description: 'query start time, format yyyy-MM-dd',
      },
      {
        name: 'endDate',
        type: 'string',
        required: false,
        description: 'query end time, format yyyy-MM-dd',
      },
      {
        name: 'limit',
        type: 'number',
        required: false,
        description: `max result count for ${device.deviceCode || 'sensor device'}`,
      },
    ],
    null,
    2,
  );

const buildEndpointResponseSchema = (device: DeviceItem, preset?: ReturnType<typeof getSensorDevicePreset>) =>
  JSON.stringify(
    {
      deviceCode: device.deviceCode,
      deviceType: device.deviceType,
      fields: preset?.dataFields || [],
    },
    null,
    2,
  );

const SensorRegistryPage: React.FC = () => {
  const [deviceForm] = Form.useForm<DeviceItem>();
  const [endpointForm] = Form.useForm<EndpointItem>();
  const [bindingForm] = Form.useForm<BindingItem>();
  const [channelForm] = Form.useForm<RealtimeChannelItem>();

  const [activeTab, setActiveTab] = useState<TabKey>('dataSources');
  const [dashboard, setDashboard] = useState<DashboardData>({});
  const [dataSources, setDataSources] = useState<DataSourceItem[]>([]);
  const [devicePage, setDevicePage] = useState<PageData<DeviceItem>>(emptyPage);
  const [endpointPage, setEndpointPage] = useState<PageData<EndpointItem>>(emptyPage);
  const [bindingPage, setBindingPage] = useState<PageData<BindingItem>>(emptyPage);
  const [channelPage, setChannelPage] = useState<PageData<RealtimeChannelItem>>(emptyPage);
  const [endpointOptions, setEndpointOptions] = useState<OptionItem[]>([]);
  const [interfaceOptions, setInterfaceOptions] = useState<OptionItem[]>([]);
  const [endpointCatalog, setEndpointCatalog] = useState<EndpointItem[]>([]);
  const [interfaceCatalog, setInterfaceCatalog] = useState<API.InterfaceInfo[]>([]);
  const [bindingCatalog, setBindingCatalog] = useState<BindingItem[]>([]);
  const [tabLoading, setTabLoading] = useState<Record<TabKey, boolean>>(emptyTabLoadingState);
  const [loadingKey, setLoadingKey] = useState('');

  const [deviceFilters, setDeviceFilters] = useState<DeviceFilter>({});
  const [endpointFilters, setEndpointFilters] = useState<EndpointFilter>({});
  const [bindingFilters, setBindingFilters] = useState<BindingFilter>({});
  const [channelFilters, setChannelFilters] = useState<ChannelFilter>({});

  const [deviceModalOpen, setDeviceModalOpen] = useState(false);
  const [endpointModalOpen, setEndpointModalOpen] = useState(false);
  const [bindingModalOpen, setBindingModalOpen] = useState(false);
  const [channelModalOpen, setChannelModalOpen] = useState(false);
  const [publishAssistantOpen, setPublishAssistantOpen] = useState(false);

  const [editingDevice, setEditingDevice] = useState<DeviceItem>();
  const [editingEndpoint, setEditingEndpoint] = useState<EndpointItem>();
  const [editingBinding, setEditingBinding] = useState<BindingItem>();
  const [editingChannel, setEditingChannel] = useState<RealtimeChannelItem>();
  const [publishingDevice, setPublishingDevice] = useState<DeviceItem>();
  const [publishSuggestion, setPublishSuggestion] = useState<PublishAssistantSuggestion>();

  const setSingleTabLoading = (tab: TabKey, value: boolean) => {
    setTabLoading((prev) => ({ ...prev, [tab]: value }));
  };

  const dataSourceMap = new Map<number, DataSourceItem>();
  dataSources.forEach((item) => {
    if (item.id) {
      dataSourceMap.set(item.id, item);
    }
  });

  const dataSourceOptions = dataSources
    .filter((item) => item.id)
    .map((item) => ({
      label: `${item.name || item.code || item.id} (${item.code || item.id})`,
      value: item.id as number,
    }));

  const formatDataSourceLabel = (dataSourceId?: number) => {
    if (!dataSourceId) {
      return '-';
    }
    const dataSource = dataSourceMap.get(dataSourceId);
    if (!dataSource) {
      return `#${dataSourceId}`;
    }
    return `${dataSource.name || dataSource.code || dataSourceId} (${dataSource.code || dataSourceId})`;
  };

  const buildPublishSuggestion = (
    device: DeviceItem,
    catalog: PublishAssistantCatalog = {
      endpoints: endpointCatalog,
      interfaces: interfaceCatalog,
      bindings: bindingCatalog,
    },
  ): PublishAssistantSuggestion => {
    const preset = getSensorDevicePreset(device.deviceType);
    const datasetOption = SENSOR_DATASET_OPTIONS.find((item) => item.key === preset?.datasetKey);
    const targetPath = normalizePath(device.dataEndpoint) || normalizePath(preset?.dataEndpoint);
    const gatewayPath = datasetOption?.gatewayPath || buildDefaultGatewayPath(targetPath);
    const issues: string[] = [];
    const notices: string[] = [];

    if (!targetPath) {
      issues.push('Missing device dataEndpoint, unable to draft the northbound target path.');
    }
    if (!gatewayPath) {
      issues.push('Unable to infer the platform gateway path from the device type or endpoint.');
    }

    const endpoint = catalog.endpoints.find(
      (item) =>
        normalizeMethod(item.method) === 'GET' &&
        ((gatewayPath && normalizePath(item.internalPath) === gatewayPath) ||
          (targetPath && normalizePath(item.targetPath) === targetPath)),
    );
    const interfaceInfo = catalog.interfaces.find(
      (item) =>
        normalizeMethod(item.method) === 'GET' &&
        gatewayPath &&
        normalizePath(item.url) === gatewayPath,
    );
    const binding =
      endpoint?.id && interfaceInfo?.id
        ? catalog.bindings.find(
            (item) =>
              item.sensorApiEndpointId === endpoint.id && item.interfaceInfoId === interfaceInfo.id,
          )
        : undefined;
    const interfaceOnline = interfaceInfo?.status === 1;

    if (
      endpoint &&
      gatewayPath &&
      normalizePath(endpoint.internalPath) &&
      normalizePath(endpoint.internalPath) !== gatewayPath
    ) {
      issues.push(
        `Existing endpoint internalPath (${endpoint.internalPath}) is different from inferred gatewayPath (${gatewayPath}). Update the endpoint before publishing.`,
      );
    }

    if (
      endpoint &&
      targetPath &&
      normalizePath(endpoint.targetPath) &&
      normalizePath(endpoint.targetPath) !== targetPath
    ) {
      issues.push(
        `Existing endpoint targetPath (${endpoint.targetPath}) is different from device targetPath (${targetPath}). Update the endpoint before publishing.`,
      );
    }
    if (interfaceInfo && !interfaceOnline) {
      notices.push('Platform interface exists but is still offline.');
    }

    const deviceLabel = device.deviceName || device.deviceCode || 'Sensor Device';
    const endpointDraft =
      !endpoint && gatewayPath && targetPath
        ? {
            endpointCode: `get_${sanitizeCodeFragment(device.deviceCode)}_data`,
            name: `${deviceLabel} Data Query`,
            description: `Northbound query draft for ${deviceLabel}.`,
            method: 'GET',
            internalPath: gatewayPath,
            targetPath,
            targetService: 'sensorhub-backend',
            category: 'northbound',
            protocolType: 'http',
            requestSchema: buildInterfaceRequestParams(device, datasetOption?.key),
            responseSchema: buildEndpointResponseSchema(device, preset),
            status: 1,
          }
        : undefined;

    const interfaceDraft =
      !interfaceInfo && gatewayPath
        ? {
            name: `${deviceLabel} Data Query`,
            description: `Platform interface draft for ${deviceLabel}.`,
            method: 'GET',
            url: gatewayPath,
            requestParams: buildInterfaceRequestParams(device, datasetOption?.key),
            requestHeader: DEFAULT_INTERFACE_REQUEST_HEADERS,
            responseHeader: DEFAULT_INTERFACE_RESPONSE_HEADERS,
          }
        : undefined;

    const bindingDraft =
      !binding && endpoint?.id && interfaceInfo?.id
        ? {
            sensorApiEndpointId: endpoint.id,
            interfaceInfoId: interfaceInfo.id,
            publishStrategy: 'proxy',
            authStrategy: 'gateway_aksk',
            status: 1,
          }
        : undefined;

    return {
      presetLabel: preset?.label,
      datasetLabel: datasetOption?.label,
      datasetKey: datasetOption?.key,
      gatewayPath,
      targetPath,
      endpoint,
      interfaceInfo,
      interfaceOnline,
      binding,
      endpointDraft,
      interfaceDraft,
      bindingDraft,
      issues,
      notices,
    };
  };

  const loadAllPageRecords = async <T,>(
    loader: (current: number, pageSize: number) => Promise<any>,
    pageSize = 50,
  ): Promise<T[]> => {
    const records: T[] = [];
    let current = 1;
    let total = 0;

    while (current <= 50) {
      const response = await loader(current, pageSize);
      const pageData = response?.data || {};
      const pageRecords = pageData.records || [];
      records.push(...pageRecords);
      total = Number(pageData.total || 0);

      if (
        !pageRecords.length ||
        pageRecords.length < pageSize ||
        (total > 0 && records.length >= total)
      ) {
        break;
      }
      current += 1;
    }

    return records;
  };

  const loadDashboard = async () => {
    try {
      const res = await getSensorRegistryDashboardUsingGET();
      setDashboard(res?.data || {});
    } catch (error: any) {
      message.error(error.message || '加载注册中心概览失败');
    }
  };

  const loadDataSources = async () => {
    setSingleTabLoading('dataSources', true);
    try {
      const res = await listSensorRegistryDataSourcesUsingGET();
      setDataSources(res?.data || []);
    } catch (error: any) {
      message.error(error.message || '加载数据源失败');
    } finally {
      setSingleTabLoading('dataSources', false);
    }
  };

  const loadDevices = async (current = 1, pageSize = 10, filters: DeviceFilter = deviceFilters) => {
    setSingleTabLoading('devices', true);
    try {
      const res = await listSensorRegistryDevicesByPageUsingGET({
        current,
        pageSize,
        ...cleanQuery(filters),
      });
      setDevicePage(res?.data || buildPageFallback<DeviceItem>(current, pageSize));
    } catch (error: any) {
      message.error(error.message || '加载设备列表失败');
    } finally {
      setSingleTabLoading('devices', false);
    }
  };

  const loadEndpoints = async (
    current = 1,
    pageSize = 10,
    filters: EndpointFilter = endpointFilters,
  ) => {
    setSingleTabLoading('endpoints', true);
    try {
      const res = await listSensorRegistryApiEndpointsByPageUsingGET({
        current,
        pageSize,
        ...cleanQuery(filters),
      });
      setEndpointPage(res?.data || buildPageFallback<EndpointItem>(current, pageSize));
    } catch (error: any) {
      message.error(error.message || '加载北向接口失败');
    } finally {
      setSingleTabLoading('endpoints', false);
    }
  };

  const loadBindings = async (current = 1, pageSize = 10, filters: BindingFilter = bindingFilters) => {
    setSingleTabLoading('bindings', true);
    try {
      const res = await listSensorRegistryBindingsByPageUsingGET({
        current,
        pageSize,
        ...cleanQuery(filters),
      });
      setBindingPage(res?.data || buildPageFallback<BindingItem>(current, pageSize));
    } catch (error: any) {
      message.error(error.message || '加载接口绑定失败');
    } finally {
      setSingleTabLoading('bindings', false);
    }
  };

  const loadChannels = async (
    current = 1,
    pageSize = 10,
    filters: ChannelFilter = channelFilters,
  ) => {
    setSingleTabLoading('realtimeChannels', true);
    try {
      const res = await listSensorRegistryRealtimeChannelsByPageUsingGET({
        current,
        pageSize,
        ...cleanQuery(filters),
      });
      setChannelPage(res?.data || buildPageFallback<RealtimeChannelItem>(current, pageSize));
    } catch (error: any) {
      message.error(error.message || '加载实时通道失败');
    } finally {
      setSingleTabLoading('realtimeChannels', false);
    }
  };

  const loadBindingOptions = async () => {
    try {
      const [endpoints, interfaces, bindings] = await Promise.all([
        loadAllPageRecords<EndpointItem>((current, pageSize) =>
          listSensorRegistryApiEndpointsByPageUsingGET({ current, pageSize }),
        ),
        loadAllPageRecords<API.InterfaceInfo>((current, pageSize) =>
          listInterfaceInfoByPageUsingGET({ current, pageSize }),
        ),
        loadAllPageRecords<BindingItem>((current, pageSize) =>
          listSensorRegistryBindingsByPageUsingGET({ current, pageSize }),
        ),
      ]);
      setEndpointCatalog(endpoints);
      setInterfaceCatalog(interfaces);
      setBindingCatalog(bindings);
      setEndpointOptions(
        endpoints.map((item: EndpointItem) => ({
          label: `${item.name || item.endpointCode} (${item.method || '-'} ${item.internalPath || '-'})`,
          value: item.id || 0,
        })),
      );
      setInterfaceOptions(
        interfaces.map((item: API.InterfaceInfo) => ({
          label: `${item.name || `接口 #${item.id}`} (${item.url || '-'})`,
          value: item.id || 0,
        })),
      );
      return { endpoints, interfaces, bindings };
    } catch (error: any) {
      message.error(error.message || '加载绑定关联选项失败');
    }
  };

  const refreshTab = async (tab: TabKey) => {
    if (tab === 'dataSources') {
      await loadDataSources();
      return;
    }
    if (tab === 'devices') {
      await loadDevices(devicePage.current || 1, devicePage.size || 10, deviceFilters);
      return;
    }
    if (tab === 'endpoints') {
      await loadEndpoints(endpointPage.current || 1, endpointPage.size || 10, endpointFilters);
      return;
    }
    if (tab === 'bindings') {
      await Promise.all([
        loadBindings(bindingPage.current || 1, bindingPage.size || 10, bindingFilters),
        loadBindingOptions(),
      ]);
      return;
    }
    await loadChannels(channelPage.current || 1, channelPage.size || 10, channelFilters);
  };

  const runAction = async (key: string, action: () => Promise<void>) => {
    setLoadingKey(key);
    try {
      await action();
    } catch (error: any) {
      message.error(error.message || '操作失败');
    } finally {
      setLoadingKey('');
    }
  };

  const refreshPublishSuggestion = async (device: DeviceItem) => {
    const catalog =
      (await loadBindingOptions()) || {
        endpoints: endpointCatalog,
        interfaces: interfaceCatalog,
        bindings: bindingCatalog,
      };
    const nextSuggestion = buildPublishSuggestion(device, catalog);
    setPublishSuggestion(nextSuggestion);
    return nextSuggestion;
  };

  const refreshPublishRelatedData = async (device?: DeviceItem) => {
    await Promise.all([
      loadDashboard(),
      loadDevices(devicePage.current || 1, devicePage.size || 10, deviceFilters),
      loadEndpoints(endpointPage.current || 1, endpointPage.size || 10, endpointFilters),
      loadBindings(bindingPage.current || 1, bindingPage.size || 10, bindingFilters),
    ]);
    if (device) {
      await refreshPublishSuggestion(device);
    } else {
      await loadBindingOptions();
    }
  };

  const summarizeSync = (label: string, result?: SyncResult) =>
    `${label}: 总数 ${result?.total || 0}，新增 ${result?.createdCount || 0}，更新 ${
      result?.updatedCount || 0
    }，跳过 ${result?.skippedCount || 0}`;

  // The page only needs a single bootstrap fetch on first mount.
  useEffect(() => {
    void runAction('init', async () => {
      await Promise.all([loadDashboard(), loadDataSources(), loadBindingOptions()]);
    });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const openDeviceModal = (record?: DeviceItem) => {
    setEditingDevice(record);
    deviceForm.setFieldsValue(
      record || {
        status: 1,
        dataSourceId: dataSources[0]?.id,
        deviceType: 'custom',
        category: 'sensor',
      },
    );
    setDeviceModalOpen(true);
  };

  const openEndpointModal = (record?: EndpointItem) => {
    setEditingEndpoint(record);
    endpointForm.setFieldsValue(
      record || {
        method: 'GET',
        targetService: 'sensorhub-backend',
        category: 'northbound',
        protocolType: 'http',
        status: 1,
      },
    );
    setEndpointModalOpen(true);
  };

  const openBindingModal = async (record?: BindingItem) => {
    await loadBindingOptions();
    setEditingBinding(record);
    bindingForm.setFieldsValue(
      record || {
        publishStrategy: 'proxy',
        authStrategy: 'gateway_aksk',
        status: 1,
      },
    );
    setBindingModalOpen(true);
  };

  const openChannelModal = (record?: RealtimeChannelItem) => {
    setEditingChannel(record);
    channelForm.setFieldsValue(record || { authType: 'session', status: 1 });
    setChannelModalOpen(true);
  };

  const openPublishAssistant = async (record: DeviceItem) => {
    setPublishingDevice(record);
    setPublishSuggestion(buildPublishSuggestion(record));
    setPublishAssistantOpen(true);
    await runAction('publishAssistantRefresh', async () => {
      await refreshPublishSuggestion(record);
    });
  };

  const createPublishEndpointDraft = async () => {
    if (!publishingDevice || !publishSuggestion?.endpointDraft) {
      message.warning('No endpoint draft is needed for the current device.');
      return;
    }
    if (publishSuggestion.issues.length > 0) {
      message.warning(publishSuggestion.issues[0]);
      return;
    }
    await runAction('publishAssistantCreateEndpoint', async () => {
      await addSensorRegistryApiEndpointUsingPOST(publishSuggestion.endpointDraft);
      message.success('Endpoint draft created.');
      await refreshPublishRelatedData(publishingDevice);
    });
  };

  const createPublishInterfaceDraft = async () => {
    if (!publishingDevice || !publishSuggestion?.interfaceDraft) {
      message.warning('No platform interface draft is needed for the current device.');
      return;
    }
    await runAction('publishAssistantCreateInterface', async () => {
      await addInterfaceInfoUsingPOST(publishSuggestion.interfaceDraft);
      message.success('Platform interface draft created.');
      await refreshPublishRelatedData(publishingDevice);
    });
  };

  const createPublishBindingDraft = async () => {
    if (!publishingDevice) {
      return;
    }
    if (publishSuggestion?.issues.length) {
      message.warning(publishSuggestion.issues[0]);
      return;
    }
    const bindingDraft =
      publishSuggestion?.bindingDraft ||
      (publishSuggestion?.endpoint?.id && publishSuggestion?.interfaceInfo?.id
        ? {
            sensorApiEndpointId: publishSuggestion.endpoint.id,
            interfaceInfoId: publishSuggestion.interfaceInfo.id,
            publishStrategy: 'proxy',
            authStrategy: 'gateway_aksk',
            status: 1,
          }
        : undefined);
    if (!bindingDraft) {
      message.warning('Please create or locate the endpoint and interface first.');
      return;
    }
    await runAction('publishAssistantCreateBinding', async () => {
      await addSensorRegistryBindingUsingPOST(bindingDraft);
      message.success('Binding draft created.');
      await refreshPublishRelatedData(publishingDevice);
    });
  };

  const onlinePublishInterface = async () => {
    if (!publishingDevice || !publishSuggestion?.interfaceInfo?.id) {
      message.warning('No platform interface is available to online.');
      return;
    }
    if (publishSuggestion.interfaceOnline) {
      message.success('Platform interface is already online.');
      return;
    }
    await runAction('publishAssistantOnlineInterface', async () => {
      await onlineInterfaceInfoUsingPOST({ id: publishSuggestion.interfaceInfo?.id });
      message.success('Platform interface is online.');
      await refreshPublishRelatedData(publishingDevice);
    });
  };

  const completePublishDrafts = async () => {
    if (!publishingDevice) {
      return;
    }
    await runAction('publishAssistantComplete', async () => {
      const latestSuggestion = await refreshPublishSuggestion(publishingDevice);
      if (latestSuggestion.issues.length > 0) {
        throw new Error(latestSuggestion.issues[0]);
      }

      let endpointId = latestSuggestion.endpoint?.id;
      if (!endpointId && latestSuggestion.endpointDraft) {
        const endpointRes = await addSensorRegistryApiEndpointUsingPOST(latestSuggestion.endpointDraft);
        endpointId = Number(endpointRes?.data);
      }

      let interfaceInfoId = latestSuggestion.interfaceInfo?.id;
      if (!interfaceInfoId && latestSuggestion.interfaceDraft) {
        const interfaceRes = await addInterfaceInfoUsingPOST(latestSuggestion.interfaceDraft);
        interfaceInfoId = Number(interfaceRes?.data);
      }

      if (!latestSuggestion.binding && endpointId && interfaceInfoId) {
        await addSensorRegistryBindingUsingPOST({
          sensorApiEndpointId: endpointId,
          interfaceInfoId,
          publishStrategy: 'proxy',
          authStrategy: 'gateway_aksk',
          status: 1,
        });
      }

      const finalSuggestion = await refreshPublishSuggestion(publishingDevice);
      if (finalSuggestion.interfaceInfo?.id && !finalSuggestion.interfaceOnline) {
        await onlineInterfaceInfoUsingPOST({ id: finalSuggestion.interfaceInfo.id });
      }

      await refreshPublishRelatedData(publishingDevice);
      message.success('Publish flow completed and the interface is ready.');
    });
  };

  const saveDevice = async () => {
    const values = await deviceForm.validateFields();
    await runAction('saveDevice', async () => {
      if (editingDevice?.id) {
        await updateSensorRegistryDeviceUsingPOST({
          ...editingDevice,
          ...values,
          id: editingDevice.id,
        });
        message.success('设备已更新');
      } else {
        await addSensorRegistryDeviceUsingPOST(values);
        message.success('设备已创建');
      }
      setDeviceModalOpen(false);
      setEditingDevice(undefined);
      await Promise.all([
        loadDashboard(),
        loadDevices(devicePage.current || 1, devicePage.size || 10, deviceFilters),
      ]);
    });
  };

  const saveEndpoint = async () => {
    const values = await endpointForm.validateFields();
    await runAction('saveEndpoint', async () => {
      if (editingEndpoint?.id) {
        await updateSensorRegistryApiEndpointUsingPOST({
          ...editingEndpoint,
          ...values,
          id: editingEndpoint.id,
        });
        message.success('北向接口已更新');
      } else {
        await addSensorRegistryApiEndpointUsingPOST(values);
        message.success('北向接口已创建');
      }
      setEndpointModalOpen(false);
      setEditingEndpoint(undefined);
      await Promise.all([
        loadDashboard(),
        loadEndpoints(endpointPage.current || 1, endpointPage.size || 10, endpointFilters),
        loadBindingOptions(),
      ]);
    });
  };

  const saveBinding = async () => {
    const values = await bindingForm.validateFields();
    await runAction('saveBinding', async () => {
      if (editingBinding?.id) {
        await updateSensorRegistryBindingUsingPOST({
          ...editingBinding,
          ...values,
          id: editingBinding.id,
        });
        message.success('接口绑定已更新');
      } else {
        await addSensorRegistryBindingUsingPOST(values);
        message.success('接口绑定已创建');
      }
      setBindingModalOpen(false);
      setEditingBinding(undefined);
      await Promise.all([
        loadDashboard(),
        loadBindings(bindingPage.current || 1, bindingPage.size || 10, bindingFilters),
      ]);
    });
  };

  const saveChannel = async () => {
    const values = await channelForm.validateFields();
    await runAction('saveChannel', async () => {
      if (editingChannel?.id) {
        await updateSensorRegistryRealtimeChannelUsingPOST({
          ...editingChannel,
          ...values,
          id: editingChannel.id,
        });
        message.success('实时通道已更新');
      } else {
        await addSensorRegistryRealtimeChannelUsingPOST(values);
        message.success('实时通道已创建');
      }
      setChannelModalOpen(false);
      setEditingChannel(undefined);
      await Promise.all([
        loadDashboard(),
        loadChannels(channelPage.current || 1, channelPage.size || 10, channelFilters),
      ]);
    });
  };

  const deleteDevice = async (id?: number) => {
    if (!id) {
      return;
    }
    await runAction('deleteDevice', async () => {
      await deleteSensorRegistryDeviceUsingPOST({ id });
      message.success('设备已删除');
      await Promise.all([
        loadDashboard(),
        loadDevices(devicePage.current || 1, devicePage.size || 10, deviceFilters),
      ]);
    });
  };

  const deleteEndpoint = async (id?: number) => {
    if (!id) {
      return;
    }
    await runAction('deleteEndpoint', async () => {
      await deleteSensorRegistryApiEndpointUsingPOST({ id });
      message.success('北向接口已删除');
      await Promise.all([
        loadDashboard(),
        loadEndpoints(endpointPage.current || 1, endpointPage.size || 10, endpointFilters),
        loadBindings(bindingPage.current || 1, bindingPage.size || 10, bindingFilters),
        loadBindingOptions(),
      ]);
    });
  };

  const deleteBinding = async (id?: number) => {
    if (!id) {
      return;
    }
    await runAction('deleteBinding', async () => {
      await deleteSensorRegistryBindingUsingPOST({ id });
      message.success('接口绑定已删除');
      await Promise.all([
        loadDashboard(),
        loadBindings(bindingPage.current || 1, bindingPage.size || 10, bindingFilters),
      ]);
    });
  };

  const deleteChannel = async (id?: number) => {
    if (!id) {
      return;
    }
    await runAction('deleteChannel', async () => {
      await deleteSensorRegistryRealtimeChannelUsingPOST({ id });
      message.success('实时通道已删除');
      await Promise.all([
        loadDashboard(),
        loadChannels(channelPage.current || 1, channelPage.size || 10, channelFilters),
      ]);
    });
  };

  const deviceColumns: ColumnsType<DeviceItem> = [
    {
      title: '设备编码',
      dataIndex: 'deviceCode',
      key: 'deviceCode',
      width: 180,
      ellipsis: true,
    },
    {
      title: '设备名称',
      dataIndex: 'deviceName',
      key: 'deviceName',
      width: 180,
      ellipsis: true,
    },
    {
      title: '设备类型',
      dataIndex: 'deviceType',
      key: 'deviceType',
      width: 140,
      render: (value) => <Tag color="blue">{value || '-'}</Tag>,
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      ellipsis: true,
    },
    {
      title: '数据源',
      dataIndex: 'dataSourceId',
      key: 'dataSourceId',
      width: 200,
      render: (value) => formatDataSourceLabel(value),
    },
    {
      title: '设备 Token',
      dataIndex: 'deviceToken',
      key: 'deviceToken',
      width: 220,
      ellipsis: true,
      render: (value) =>
        value ? <Typography.Text copyable>{String(value)}</Typography.Text> : '-',
    },
    {
      title: '数据接口',
      dataIndex: 'dataEndpoint',
      key: 'dataEndpoint',
      width: 220,
      ellipsis: true,
    },
    {
      title: '实时 Key',
      dataIndex: 'realtimeKey',
      key: 'realtimeKey',
      width: 180,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value) => renderStatusTag(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 300,
      fixed: 'right',
      render: (_, record) => (
        <Space wrap>
          <Button size="small" type="link" onClick={() => void openPublishAssistant(record)}>
            Publish Assistant
          </Button>
          <Button size="small" type="link" onClick={() => openDeviceModal(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除该设备吗？" onConfirm={() => void deleteDevice(record.id)}>
            <Button size="small" type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const endpointColumns: ColumnsType<EndpointItem> = [
    {
      title: '接口编码',
      dataIndex: 'endpointCode',
      key: 'endpointCode',
      width: 220,
      ellipsis: true,
    },
    {
      title: '接口名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      ellipsis: true,
    },
    {
      title: 'Method',
      dataIndex: 'method',
      key: 'method',
      width: 110,
      render: (value) => <Tag color="blue">{value || '-'}</Tag>,
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      ellipsis: true,
    },
    {
      title: '内部路径',
      dataIndex: 'internalPath',
      key: 'internalPath',
      width: 220,
      ellipsis: true,
    },
    {
      title: '目标服务',
      dataIndex: 'targetService',
      key: 'targetService',
      width: 160,
      ellipsis: true,
    },
    {
      title: '目标路径',
      dataIndex: 'targetPath',
      key: 'targetPath',
      width: 220,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value) => renderStatusTag(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button size="small" type="link" onClick={() => openEndpointModal(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除该北向接口吗？" onConfirm={() => void deleteEndpoint(record.id)}>
            <Button size="small" type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const bindingColumns: ColumnsType<BindingItem> = [
    {
      title: '北向接口',
      dataIndex: 'endpointName',
      key: 'endpointName',
      width: 240,
      render: (_, record) => record.endpointName || record.endpointCode || '-',
    },
    {
      title: 'Method',
      dataIndex: 'method',
      key: 'method',
      width: 100,
      render: (value) => <Tag color="blue">{value || '-'}</Tag>,
    },
    {
      title: '内部路径',
      dataIndex: 'internalPath',
      key: 'internalPath',
      width: 220,
      ellipsis: true,
    },
    {
      title: '平台接口',
      dataIndex: 'interfaceName',
      key: 'interfaceName',
      width: 260,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{record.interfaceName || `接口 #${record.interfaceInfoId || '-'}`}</Typography.Text>
          <Typography.Text type="secondary">{record.interfaceUrl || '-'}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '发布策略',
      dataIndex: 'publishStrategy',
      key: 'publishStrategy',
      width: 120,
      ellipsis: true,
    },
    {
      title: '绑定状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value) => renderStatusTag(value),
    },
    {
      title: '平台状态',
      dataIndex: 'interfaceStatus',
      key: 'interfaceStatus',
      width: 110,
      render: (value) => renderStatusTag(value, '已发布', '已下线'),
    },
    {
      title: '操作',
      key: 'action',
      width: 320,
      fixed: 'right',
      render: (_, record) => (
        <Space wrap>
          {record.interfaceInfoId ? (
            <Button
              size="small"
              type="link"
              onClick={() => history.push(`/interface_info/${record.interfaceInfoId}`)}
            >
              查看接口
            </Button>
          ) : null}
          <Button size="small" type="link" onClick={() => history.push('/admin/interface_info')}>
            接口管理
          </Button>
          <Button size="small" type="link" onClick={() => void openBindingModal(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除该接口绑定吗？" onConfirm={() => void deleteBinding(record.id)}>
            <Button size="small" type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const channelColumns: ColumnsType<RealtimeChannelItem> = [
    {
      title: '通道编码',
      dataIndex: 'channelCode',
      key: 'channelCode',
      width: 220,
      ellipsis: true,
    },
    {
      title: '通道名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      ellipsis: true,
    },
    {
      title: 'WebSocket 地址',
      dataIndex: 'wsPath',
      key: 'wsPath',
      width: 260,
      ellipsis: true,
    },
    {
      title: 'Topic',
      dataIndex: 'topic',
      key: 'topic',
      width: 180,
      ellipsis: true,
    },
    {
      title: '设备类型',
      dataIndex: 'deviceType',
      key: 'deviceType',
      width: 160,
      ellipsis: true,
    },
    {
      title: '鉴权方式',
      dataIndex: 'authType',
      key: 'authType',
      width: 120,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value) => renderStatusTag(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button size="small" type="link" onClick={() => openChannelModal(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除该实时通道吗？" onConfirm={() => void deleteChannel(record.id)}>
            <Button size="small" type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const renderDeviceFilters = () => (
    <Space wrap style={{ marginBottom: 16 }}>
      <Input
        allowClear
        placeholder="设备编码"
        style={{ width: 180 }}
        value={deviceFilters.deviceCode}
        onChange={(event) =>
          setDeviceFilters((prev) => ({ ...prev, deviceCode: event.target.value || undefined }))
        }
        onPressEnter={() => void loadDevices(1, devicePage.size || 10, deviceFilters)}
      />
      <Input
        allowClear
        placeholder="设备名称"
        style={{ width: 180 }}
        value={deviceFilters.deviceName}
        onChange={(event) =>
          setDeviceFilters((prev) => ({ ...prev, deviceName: event.target.value || undefined }))
        }
        onPressEnter={() => void loadDevices(1, devicePage.size || 10, deviceFilters)}
      />
      <Input
        allowClear
        placeholder="设备类型"
        style={{ width: 160 }}
        value={deviceFilters.deviceType}
        onChange={(event) =>
          setDeviceFilters((prev) => ({ ...prev, deviceType: event.target.value || undefined }))
        }
        onPressEnter={() => void loadDevices(1, devicePage.size || 10, deviceFilters)}
      />
      <Select
        allowClear
        placeholder="数据源"
        style={{ width: 220 }}
        options={dataSourceOptions}
        value={deviceFilters.dataSourceId}
        onChange={(value) => setDeviceFilters((prev) => ({ ...prev, dataSourceId: value }))}
      />
      <Select
        allowClear
        placeholder="状态"
        style={{ width: 120 }}
        options={STATUS_OPTIONS}
        value={deviceFilters.status}
        onChange={(value) => setDeviceFilters((prev) => ({ ...prev, status: value }))}
      />
      <Button type="primary" onClick={() => void loadDevices(1, devicePage.size || 10, deviceFilters)}>
        查询
      </Button>
      <Button
        onClick={() => {
          const nextFilters = {};
          setDeviceFilters(nextFilters);
          void loadDevices(1, devicePage.size || 10, nextFilters);
        }}
      >
        重置
      </Button>
    </Space>
  );

  const renderEndpointFilters = () => (
    <Space wrap style={{ marginBottom: 16 }}>
      <Input
        allowClear
        placeholder="接口编码"
        style={{ width: 180 }}
        value={endpointFilters.endpointCode}
        onChange={(event) =>
          setEndpointFilters((prev) => ({ ...prev, endpointCode: event.target.value || undefined }))
        }
        onPressEnter={() => void loadEndpoints(1, endpointPage.size || 10, endpointFilters)}
      />
      <Input
        allowClear
        placeholder="接口名称"
        style={{ width: 180 }}
        value={endpointFilters.name}
        onChange={(event) =>
          setEndpointFilters((prev) => ({ ...prev, name: event.target.value || undefined }))
        }
        onPressEnter={() => void loadEndpoints(1, endpointPage.size || 10, endpointFilters)}
      />
      <Select
        allowClear
        placeholder="Method"
        style={{ width: 120 }}
        options={METHOD_OPTIONS}
        value={endpointFilters.method}
        onChange={(value) => setEndpointFilters((prev) => ({ ...prev, method: value }))}
      />
      <Input
        allowClear
        placeholder="目标服务"
        style={{ width: 160 }}
        value={endpointFilters.targetService}
        onChange={(event) =>
          setEndpointFilters((prev) => ({ ...prev, targetService: event.target.value || undefined }))
        }
        onPressEnter={() => void loadEndpoints(1, endpointPage.size || 10, endpointFilters)}
      />
      <Input
        allowClear
        placeholder="分类"
        style={{ width: 140 }}
        value={endpointFilters.category}
        onChange={(event) =>
          setEndpointFilters((prev) => ({ ...prev, category: event.target.value || undefined }))
        }
        onPressEnter={() => void loadEndpoints(1, endpointPage.size || 10, endpointFilters)}
      />
      <Select
        allowClear
        placeholder="状态"
        style={{ width: 120 }}
        options={STATUS_OPTIONS}
        value={endpointFilters.status}
        onChange={(value) => setEndpointFilters((prev) => ({ ...prev, status: value }))}
      />
      <Button
        type="primary"
        onClick={() => void loadEndpoints(1, endpointPage.size || 10, endpointFilters)}
      >
        查询
      </Button>
      <Button
        onClick={() => {
          const nextFilters = {};
          setEndpointFilters(nextFilters);
          void loadEndpoints(1, endpointPage.size || 10, nextFilters);
        }}
      >
        重置
      </Button>
    </Space>
  );

  const renderBindingFilters = () => (
    <Space wrap style={{ marginBottom: 16 }}>
      <Select
        showSearch
        allowClear
        optionFilterProp="label"
        placeholder="选择北向接口"
        style={{ width: 320 }}
        options={endpointOptions}
        value={bindingFilters.sensorApiEndpointId}
        onChange={(value) =>
          setBindingFilters((prev) => ({ ...prev, sensorApiEndpointId: value }))
        }
      />
      <Select
        showSearch
        allowClear
        optionFilterProp="label"
        placeholder="选择平台接口"
        style={{ width: 320 }}
        options={interfaceOptions}
        value={bindingFilters.interfaceInfoId}
        onChange={(value) => setBindingFilters((prev) => ({ ...prev, interfaceInfoId: value }))}
      />
      <Select
        allowClear
        placeholder="状态"
        style={{ width: 120 }}
        options={STATUS_OPTIONS}
        value={bindingFilters.status}
        onChange={(value) => setBindingFilters((prev) => ({ ...prev, status: value }))}
      />
      <Button type="primary" onClick={() => void loadBindings(1, bindingPage.size || 10, bindingFilters)}>
        查询
      </Button>
      <Button
        onClick={() => {
          const nextFilters = {};
          setBindingFilters(nextFilters);
          void loadBindings(1, bindingPage.size || 10, nextFilters);
        }}
      >
        重置
      </Button>
    </Space>
  );

  const renderChannelFilters = () => (
    <Space wrap style={{ marginBottom: 16 }}>
      <Input
        allowClear
        placeholder="通道编码"
        style={{ width: 180 }}
        value={channelFilters.channelCode}
        onChange={(event) =>
          setChannelFilters((prev) => ({ ...prev, channelCode: event.target.value || undefined }))
        }
        onPressEnter={() => void loadChannels(1, channelPage.size || 10, channelFilters)}
      />
      <Input
        allowClear
        placeholder="通道名称"
        style={{ width: 180 }}
        value={channelFilters.name}
        onChange={(event) =>
          setChannelFilters((prev) => ({ ...prev, name: event.target.value || undefined }))
        }
        onPressEnter={() => void loadChannels(1, channelPage.size || 10, channelFilters)}
      />
      <Input
        allowClear
        placeholder="设备类型"
        style={{ width: 160 }}
        value={channelFilters.deviceType}
        onChange={(event) =>
          setChannelFilters((prev) => ({ ...prev, deviceType: event.target.value || undefined }))
        }
        onPressEnter={() => void loadChannels(1, channelPage.size || 10, channelFilters)}
      />
      <Input
        allowClear
        placeholder="鉴权方式"
        style={{ width: 160 }}
        value={channelFilters.authType}
        onChange={(event) =>
          setChannelFilters((prev) => ({ ...prev, authType: event.target.value || undefined }))
        }
        onPressEnter={() => void loadChannels(1, channelPage.size || 10, channelFilters)}
      />
      <Select
        allowClear
        placeholder="状态"
        style={{ width: 120 }}
        options={STATUS_OPTIONS}
        value={channelFilters.status}
        onChange={(value) => setChannelFilters((prev) => ({ ...prev, status: value }))}
      />
      <Button
        type="primary"
        onClick={() => void loadChannels(1, channelPage.size || 10, channelFilters)}
      >
        查询
      </Button>
      <Button
        onClick={() => {
          const nextFilters = {};
          setChannelFilters(nextFilters);
          void loadChannels(1, channelPage.size || 10, nextFilters);
        }}
      >
        重置
      </Button>
    </Space>
  );

  const publishAssistantNeedsEndpoint = Boolean(publishSuggestion?.endpointDraft);
  const publishAssistantNeedsInterface = Boolean(publishSuggestion?.interfaceDraft);
  const publishAssistantNeedsBinding = Boolean(
    publishSuggestion?.bindingDraft ||
      (publishSuggestion?.endpoint?.id && publishSuggestion?.interfaceInfo?.id && !publishSuggestion?.binding),
  );
  const publishAssistantNeedsOnline = Boolean(
    publishSuggestion?.interfaceInfo?.id && !publishSuggestion?.interfaceOnline,
  );
  const publishAssistantHasIssues = (publishSuggestion?.issues.length || 0) > 0;
  const publishAssistantComplete = Boolean(
    publishSuggestion?.endpoint &&
      publishSuggestion?.interfaceInfo &&
      publishSuggestion?.binding &&
      publishSuggestion?.interfaceOnline,
  );

  return (
    <PageContainer title="SensorHub 注册中心">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="Phase 3：SensorHub 注册中心已进入 Spring Boot 主导阶段"
          description="设备、北向接口、接口绑定、实时通道现在都可以直接落到 MySQL 注册中心里管理。Sync / Bootstrap 主要用于本地元数据初始化和一致性对账。"
        />

        <Card>
          <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text>
              这里是 SensorHub 统一接管传感器元数据的后台入口。你可以直接维护设备、接口发布模型和实时通道，不再需要额外的独立管理端。
            </Typography.Text>
            <Space wrap>
              <Button
                onClick={() =>
                  void runAction('refreshAll', async () => {
                    await Promise.all([loadDashboard(), loadDataSources(), refreshTab(activeTab)]);
                  })
                }
                loading={loadingKey === 'refreshAll'}
              >
                刷新
              </Button>
              <Button
                type="primary"
                onClick={() =>
                  void runAction('bootstrap', async () => {
                    const res = await bootstrapSensorRegistryUsingPOST();
                    const data = res?.data || {};
                    message.success(
                      [
                        summarizeSync('设备', data.deviceSync),
                        summarizeSync('北向接口', data.endpointSync),
                        summarizeSync('接口绑定', data.bindingSync),
                        summarizeSync('实时通道', data.realtimeChannelSync),
                      ].join(' | '),
                    );
                    await Promise.all([
                      loadDashboard(),
                      loadDataSources(),
                      loadDevices(1, devicePage.size || 10, deviceFilters),
                      loadEndpoints(1, endpointPage.size || 10, endpointFilters),
                      loadBindings(1, bindingPage.size || 10, bindingFilters),
                      loadChannels(1, channelPage.size || 10, channelFilters),
                      loadBindingOptions(),
                    ]);
                  })
                }
                loading={loadingKey === 'bootstrap'}
              >
                Bootstrap
              </Button>
              <Button
                onClick={() =>
                  void runAction('syncDevices', async () => {
                    const res = await syncSensorRegistryDevicesUsingPOST();
                    message.success(summarizeSync('设备', res?.data));
                    await Promise.all([
                      loadDashboard(),
                      loadDevices(devicePage.current || 1, devicePage.size || 10, deviceFilters),
                    ]);
                  })
                }
                loading={loadingKey === 'syncDevices'}
              >
                Sync 设备
              </Button>
              <Button
                onClick={() =>
                  void runAction('syncEndpoints', async () => {
                    const res = await syncSensorRegistryEndpointsUsingPOST();
                    message.success(summarizeSync('北向接口', res?.data));
                    await Promise.all([
                      loadDashboard(),
                      loadEndpoints(endpointPage.current || 1, endpointPage.size || 10, endpointFilters),
                      loadBindingOptions(),
                    ]);
                  })
                }
                loading={loadingKey === 'syncEndpoints'}
              >
                Sync 接口
              </Button>
              <Button
                onClick={() =>
                  void runAction('syncBindings', async () => {
                    const res = await syncSensorRegistryBindingsUsingPOST();
                    message.success(summarizeSync('接口绑定', res?.data));
                    await Promise.all([
                      loadDashboard(),
                      loadBindings(bindingPage.current || 1, bindingPage.size || 10, bindingFilters),
                    ]);
                  })
                }
                loading={loadingKey === 'syncBindings'}
              >
                Sync 绑定
              </Button>
              <Button
                onClick={() =>
                  void runAction('syncChannels', async () => {
                    const res = await syncSensorRegistryRealtimeChannelsUsingPOST();
                    message.success(summarizeSync('实时通道', res?.data));
                    await Promise.all([
                      loadDashboard(),
                      loadChannels(channelPage.current || 1, channelPage.size || 10, channelFilters),
                    ]);
                  })
                }
                loading={loadingKey === 'syncChannels'}
              >
                Sync 通道
              </Button>
            </Space>
          </Space>
        </Card>

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} md={8} xl={4}>
            <Card>
              <Typography.Text type="secondary">数据源</Typography.Text>
              <Typography.Title level={3}>{dashboard.dataSourceCount || 0}</Typography.Title>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} xl={4}>
            <Card>
              <Typography.Text type="secondary">设备</Typography.Text>
              <Typography.Title level={3}>{dashboard.deviceCount || 0}</Typography.Title>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} xl={4}>
            <Card>
              <Typography.Text type="secondary">北向接口</Typography.Text>
              <Typography.Title level={3}>{dashboard.endpointCount || 0}</Typography.Title>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} xl={4}>
            <Card>
              <Typography.Text type="secondary">接口绑定</Typography.Text>
              <Typography.Title level={3}>{dashboard.bindingCount || 0}</Typography.Title>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} xl={4}>
            <Card>
              <Typography.Text type="secondary">实时通道</Typography.Text>
              <Typography.Title level={3}>{dashboard.realtimeChannelCount || 0}</Typography.Title>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} xl={4}>
            <Card>
              <Typography.Text type="secondary">已发布平台接口</Typography.Text>
              <Typography.Title level={3}>{dashboard.publishedInterfaceCount || 0}</Typography.Title>
            </Card>
          </Col>
        </Row>

        <Card>
          <Descriptions column={2}>
            <Descriptions.Item label="默认数据源">{dashboard.defaultDataSourceCode || '-'}</Descriptions.Item>
            <Descriptions.Item label="当前 Tab">{activeTab}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card
          title="注册中心详情"
          extra={
            <Space wrap>
              <Button
                onClick={() => void runAction('refreshTab', async () => refreshTab(activeTab))}
                loading={loadingKey === 'refreshTab'}
              >
                刷新当前 Tab
              </Button>
              {activeTab === 'devices' ? (
                <>
                  <Button onClick={() => history.push('/sensor/devices')}>打开设备页</Button>
                  <Button type="primary" onClick={() => openDeviceModal()}>
                    新建设备
                  </Button>
                </>
              ) : null}
              {activeTab === 'endpoints' ? (
                <Button type="primary" onClick={() => openEndpointModal()}>
                  新建北向接口
                </Button>
              ) : null}
              {activeTab === 'bindings' ? (
                <Button type="primary" onClick={() => void openBindingModal()}>
                  新建接口绑定
                </Button>
              ) : null}
              {activeTab === 'realtimeChannels' ? (
                <Button type="primary" onClick={() => openChannelModal()}>
                  新建实时通道
                </Button>
              ) : null}
            </Space>
          }
        >
          <Tabs
            activeKey={activeTab}
            onChange={(key) => {
              setActiveTab(key as TabKey);
              void refreshTab(key as TabKey);
            }}
          >
            <Tabs.TabPane tab="数据源" key="dataSources">
              <Table<DataSourceItem>
                rowKey={(record) => record.id || record.code || 'data-source'}
                loading={tabLoading.dataSources || loadingKey === 'init'}
                columns={[
                  { title: '编码', dataIndex: 'code', key: 'code', width: 180 },
                  { title: '名称', dataIndex: 'name', key: 'name', width: 180 },
                  {
                    title: '类型',
                    dataIndex: 'type',
                    key: 'type',
                    width: 120,
                    render: (value) => <Tag color="blue">{value || '-'}</Tag>,
                  },
                  { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl', width: 220, ellipsis: true },
                  { title: 'WebSocket URL', dataIndex: 'wsUrl', key: 'wsUrl', width: 240, ellipsis: true },
                  {
                    title: '状态',
                    dataIndex: 'status',
                    key: 'status',
                    width: 100,
                    render: (value) => renderStatusTag(value),
                  },
                ]}
                dataSource={dataSources}
                pagination={false}
                scroll={{ x: 1300 }}
              />
            </Tabs.TabPane>

            <Tabs.TabPane tab="设备" key="devices">
              {renderDeviceFilters()}
              <Table<DeviceItem>
                rowKey={(record) => record.id || record.deviceCode || 'device'}
                loading={tabLoading.devices}
                columns={deviceColumns}
                dataSource={devicePage.records || []}
                pagination={{
                  current: devicePage.current || 1,
                  pageSize: devicePage.size || 10,
                  total: devicePage.total || 0,
                  showSizeChanger: true,
                }}
                onChange={(pagination: TablePaginationConfig) =>
                  void loadDevices(pagination.current || 1, pagination.pageSize || 10, deviceFilters)
                }
                scroll={{ x: 2060 }}
              />
            </Tabs.TabPane>

            <Tabs.TabPane tab="北向接口" key="endpoints">
              {renderEndpointFilters()}
              <Table<EndpointItem>
                rowKey={(record) => record.id || record.endpointCode || 'endpoint'}
                loading={tabLoading.endpoints}
                columns={endpointColumns}
                dataSource={endpointPage.records || []}
                pagination={{
                  current: endpointPage.current || 1,
                  pageSize: endpointPage.size || 10,
                  total: endpointPage.total || 0,
                  showSizeChanger: true,
                }}
                onChange={(pagination: TablePaginationConfig) =>
                  void loadEndpoints(pagination.current || 1, pagination.pageSize || 10, endpointFilters)
                }
                scroll={{ x: 1900 }}
              />
            </Tabs.TabPane>

            <Tabs.TabPane tab="接口绑定" key="bindings">
              {renderBindingFilters()}
              <Table<BindingItem>
                rowKey={(record) => record.id || `${record.sensorApiEndpointId}-${record.interfaceInfoId}`}
                loading={tabLoading.bindings}
                columns={bindingColumns}
                dataSource={bindingPage.records || []}
                pagination={{
                  current: bindingPage.current || 1,
                  pageSize: bindingPage.size || 10,
                  total: bindingPage.total || 0,
                  showSizeChanger: true,
                }}
                onChange={(pagination: TablePaginationConfig) =>
                  void loadBindings(pagination.current || 1, pagination.pageSize || 10, bindingFilters)
                }
                scroll={{ x: 2100 }}
              />
            </Tabs.TabPane>

            <Tabs.TabPane tab="实时通道" key="realtimeChannels">
              {renderChannelFilters()}
              <Table<RealtimeChannelItem>
                rowKey={(record) => record.id || record.channelCode || 'channel'}
                loading={tabLoading.realtimeChannels}
                columns={channelColumns}
                dataSource={channelPage.records || []}
                pagination={{
                  current: channelPage.current || 1,
                  pageSize: channelPage.size || 10,
                  total: channelPage.total || 0,
                  showSizeChanger: true,
                }}
                onChange={(pagination: TablePaginationConfig) =>
                  void loadChannels(pagination.current || 1, pagination.pageSize || 10, channelFilters)
                }
                scroll={{ x: 1820 }}
              />
            </Tabs.TabPane>
          </Tabs>
        </Card>
      </Space>

      <Modal
        open={publishAssistantOpen}
        title="Publish Assistant"
        onCancel={() => {
          setPublishAssistantOpen(false);
          setPublishingDevice(undefined);
          setPublishSuggestion(undefined);
        }}
        footer={
          <Space wrap>
            <Button
              onClick={() => {
                setPublishAssistantOpen(false);
                setPublishingDevice(undefined);
                setPublishSuggestion(undefined);
              }}
            >
              Close
            </Button>
            <Button
              onClick={() =>
                publishingDevice
                  ? void runAction('publishAssistantRefresh', async () => {
                      await refreshPublishSuggestion(publishingDevice);
                    })
                  : undefined
              }
              loading={loadingKey === 'publishAssistantRefresh'}
            >
              Refresh Suggestion
            </Button>
            <Button
              onClick={() => void createPublishEndpointDraft()}
              disabled={!publishAssistantNeedsEndpoint || publishAssistantHasIssues}
              loading={loadingKey === 'publishAssistantCreateEndpoint'}
            >
              Create Endpoint Draft
            </Button>
            <Button
              onClick={() => void createPublishInterfaceDraft()}
              disabled={!publishAssistantNeedsInterface}
              loading={loadingKey === 'publishAssistantCreateInterface'}
            >
              Create Interface Draft
            </Button>
            <Button
              onClick={() => void createPublishBindingDraft()}
              disabled={!publishAssistantNeedsBinding || publishAssistantHasIssues}
              loading={loadingKey === 'publishAssistantCreateBinding'}
            >
              Create Binding Draft
            </Button>
            <Button
              onClick={() => void onlinePublishInterface()}
              disabled={!publishAssistantNeedsOnline}
              loading={loadingKey === 'publishAssistantOnlineInterface'}
            >
              Online Interface
            </Button>
            <Button
              type="primary"
              onClick={() => void completePublishDrafts()}
              disabled={publishAssistantComplete || publishAssistantHasIssues}
              loading={loadingKey === 'publishAssistantComplete'}
            >
              One-click Complete
            </Button>
          </Space>
        }
        destroyOnClose
        width={960}
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          {publishingDevice ? (
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="Device">
                {publishingDevice.deviceName || publishingDevice.deviceCode || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Device Code">
                {publishingDevice.deviceCode || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Device Type">
                {publishingDevice.deviceType || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Preset">
                {publishSuggestion?.presetLabel || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Dataset">
                {publishSuggestion?.datasetLabel || publishSuggestion?.datasetKey || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Target Path">
                {publishSuggestion?.targetPath ? (
                  <Typography.Text copyable>{publishSuggestion.targetPath}</Typography.Text>
                ) : (
                  '-'
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Gateway Path" span={2}>
                {publishSuggestion?.gatewayPath ? (
                  <Typography.Text copyable>{publishSuggestion.gatewayPath}</Typography.Text>
                ) : (
                  '-'
                )}
              </Descriptions.Item>
            </Descriptions>
          ) : null}

          <Alert
            showIcon
            type={publishAssistantComplete ? 'success' : publishAssistantHasIssues ? 'warning' : 'info'}
            message={
              publishAssistantComplete
                ? 'This device is fully published and the platform interface is online.'
                : 'Publish assistant analysed the current device and registry records.'
            }
            description={
              publishAssistantHasIssues
                ? publishSuggestion?.issues.join(' ')
                : [
                    publishAssistantNeedsEndpoint ? 'endpoint draft missing' : 'endpoint ready',
                    publishAssistantNeedsInterface ? 'interface draft missing' : 'interface ready',
                    publishAssistantNeedsBinding ? 'binding draft missing' : 'binding ready',
                    publishAssistantNeedsOnline ? 'interface offline' : 'interface online',
                  ].join(' | ')
            }
          />

          {publishSuggestion?.notices.length ? (
            <Alert
              showIcon
              type="warning"
              message="Publish Notices"
              description={publishSuggestion.notices.join(' ')}
            />
          ) : null}

          <Row gutter={[16, 16]}>
            <Col xs={24} md={8}>
              <Card size="small" title="Endpoint">
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  {publishSuggestion?.endpoint ? (
                    <>
                      <Tag color="green">Ready</Tag>
                      <Typography.Text>{publishSuggestion.endpoint.name || '-'}</Typography.Text>
                      <Typography.Text type="secondary">
                        {publishSuggestion.endpoint.method || '-'} {publishSuggestion.endpoint.internalPath || '-'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        target: {publishSuggestion.endpoint.targetPath || '-'}
                      </Typography.Text>
                    </>
                  ) : (
                    <>
                      <Tag color="orange">Draft Needed</Tag>
                      <Typography.Text>
                        {publishSuggestion?.endpointDraft?.name || 'No endpoint draft available'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        {publishSuggestion?.endpointDraft?.method || '-'}{' '}
                        {publishSuggestion?.endpointDraft?.internalPath || '-'}
                      </Typography.Text>
                    </>
                  )}
                </Space>
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card size="small" title="Platform Interface">
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  {publishSuggestion?.interfaceInfo ? (
                    <>
                      <Space wrap size={8}>
                        <Tag color="green">Ready</Tag>
                        <Tag color={publishSuggestion.interfaceOnline ? 'green' : 'orange'}>
                          {publishSuggestion.interfaceOnline ? 'Online' : 'Offline'}
                        </Tag>
                      </Space>
                      <Typography.Text>{publishSuggestion.interfaceInfo.name || '-'}</Typography.Text>
                      <Typography.Text type="secondary">
                        {publishSuggestion.interfaceInfo.method || '-'} {publishSuggestion.interfaceInfo.url || '-'}
                      </Typography.Text>
                      {publishSuggestion.interfaceInfo.id ? (
                        <Button
                          type="link"
                          size="small"
                          style={{ padding: 0 }}
                          onClick={() => history.push(`/interface_info/${publishSuggestion.interfaceInfo?.id}`)}
                        >
                          Open Interface
                        </Button>
                      ) : null}
                    </>
                  ) : (
                    <>
                      <Tag color="orange">Draft Needed</Tag>
                      <Typography.Text>
                        {publishSuggestion?.interfaceDraft?.name || 'No interface draft available'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        {publishSuggestion?.interfaceDraft?.method || '-'}{' '}
                        {publishSuggestion?.interfaceDraft?.url || '-'}
                      </Typography.Text>
                    </>
                  )}
                </Space>
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card size="small" title="Binding">
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  {publishSuggestion?.binding ? (
                    <>
                      <Tag color="green">Ready</Tag>
                      <Typography.Text>
                        endpoint #{publishSuggestion.binding.sensorApiEndpointId || '-'} {'->'} interface #
                        {publishSuggestion.binding.interfaceInfoId || '-'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        {publishSuggestion.binding.publishStrategy || 'proxy'} /{' '}
                        {publishSuggestion.binding.authStrategy || 'gateway_aksk'}
                      </Typography.Text>
                    </>
                  ) : (
                    <>
                      <Tag color="orange">Draft Needed</Tag>
                      <Typography.Text>
                        {publishAssistantNeedsBinding
                          ? 'Binding can be created after endpoint and interface are ready.'
                          : 'Waiting for endpoint and interface records.'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        endpoint #{publishSuggestion?.endpoint?.id || '-'} / interface #
                        {publishSuggestion?.interfaceInfo?.id || '-'}
                      </Typography.Text>
                    </>
                  )}
                </Space>
              </Card>
            </Col>
          </Row>
        </Space>
      </Modal>

      <Modal
        open={deviceModalOpen}
        title={editingDevice?.id ? '编辑设备' : '新建设备'}
        onCancel={() => {
          setDeviceModalOpen(false);
          setEditingDevice(undefined);
        }}
        onOk={() => void saveDevice()}
        confirmLoading={loadingKey === 'saveDevice'}
        destroyOnClose
        width={840}
      >
        <Form form={deviceForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="deviceCode" label="设备编码" rules={[{ required: true, message: '请输入设备编码' }]}>
                <Input placeholder="例如：sensor_gps_001" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deviceName" label="设备名称" rules={[{ required: true, message: '请输入设备名称' }]}>
                <Input placeholder="例如：无人机 GPS 设备" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deviceType" label="设备类型" rules={[{ required: true, message: '请输入设备类型' }]}>
                <Input placeholder="例如：gps / camera / weather" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="category" label="设备分类">
                <Input placeholder="例如：sensor" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="dataSourceId"
                label="所属数据源"
                rules={[{ required: true, message: '请选择数据源' }]}
              >
                <Select options={dataSourceOptions} placeholder="选择数据源" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
                <Select options={STATUS_OPTIONS} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="deviceToken"
                label="设备 Token"
                rules={[{ required: true, message: '请输入设备 Token' }]}
              >
                <Input placeholder="设备鉴权标识" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="legacyDeviceId" label="历史设备 ID">
                <Input placeholder="保留历史系统设备标识时可填写" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="dataEndpoint" label="数据接口路径">
                <Input placeholder="/api/sensor/device/data" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="realtimeKey" label="实时 Key">
                <Input placeholder="gps / camera / weather" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="description" label="描述">
                <Input.TextArea rows={2} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="configJson" label="配置 JSON">
                <Input.TextArea rows={4} placeholder='{"managedBy":"SensorHub"}' />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="dataFieldsJson" label="字段 JSON">
                <Input.TextArea rows={4} placeholder='[{"field":"lat","type":"number"}]' />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        open={endpointModalOpen}
        title={editingEndpoint?.id ? '编辑北向接口' : '新建北向接口'}
        onCancel={() => {
          setEndpointModalOpen(false);
          setEditingEndpoint(undefined);
        }}
        onOk={() => void saveEndpoint()}
        confirmLoading={loadingKey === 'saveEndpoint'}
        destroyOnClose
        width={820}
      >
        <Form form={endpointForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="endpointCode" label="接口编码" rules={[{ required: true, message: '请输入接口编码' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="name" label="接口名称" rules={[{ required: true, message: '请输入接口名称' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="method" label="Method" rules={[{ required: true, message: '请选择 Method' }]}>
                <Select options={METHOD_OPTIONS} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
                <Select options={STATUS_OPTIONS} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="internalPath" label="内部路径" rules={[{ required: true, message: '请输入内部路径' }]}>
                <Input placeholder="/api/sensor/example" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="targetPath" label="目标路径" rules={[{ required: true, message: '请输入目标路径' }]}>
                <Input placeholder="/api/example" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="targetService" label="目标服务">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="category" label="分类">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="protocolType" label="协议类型">
                <Input />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="description" label="描述">
                <Input.TextArea rows={2} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="requestSchema" label="请求 Schema">
                <Input.TextArea rows={3} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="responseSchema" label="响应 Schema">
                <Input.TextArea rows={3} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        open={bindingModalOpen}
        title={editingBinding?.id ? '编辑接口绑定' : '新建接口绑定'}
        onCancel={() => {
          setBindingModalOpen(false);
          setEditingBinding(undefined);
        }}
        onOk={() => void saveBinding()}
        confirmLoading={loadingKey === 'saveBinding'}
        destroyOnClose
        width={760}
      >
        <Form form={bindingForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="sensorApiEndpointId"
                label="北向接口"
                rules={[{ required: true, message: '请选择北向接口' }]}
              >
                <Select showSearch optionFilterProp="label" options={endpointOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="interfaceInfoId"
                label="平台接口"
                rules={[{ required: true, message: '请选择平台接口' }]}
              >
                <Select showSearch optionFilterProp="label" options={interfaceOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="publishStrategy" label="发布策略">
                <Select
                  options={['proxy', 'passthrough', 'aggregate'].map((value) => ({
                    label: value,
                    value,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="authStrategy" label="鉴权策略">
                <Select
                  options={['gateway_aksk', 'session', 'internal'].map((value) => ({
                    label: value,
                    value,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="limitStrategy" label="限流策略">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="cacheStrategy" label="缓存策略">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
                <Select options={STATUS_OPTIONS} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        open={channelModalOpen}
        title={editingChannel?.id ? '编辑实时通道' : '新建实时通道'}
        onCancel={() => {
          setChannelModalOpen(false);
          setEditingChannel(undefined);
        }}
        onOk={() => void saveChannel()}
        confirmLoading={loadingKey === 'saveChannel'}
        destroyOnClose
        width={760}
      >
        <Form form={channelForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="channelCode" label="通道编码" rules={[{ required: true, message: '请输入通道编码' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="name" label="通道名称" rules={[{ required: true, message: '请输入通道名称' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="wsPath" label="WebSocket 地址" rules={[{ required: true, message: '请输入 WebSocket 地址' }]}>
                <Input placeholder="ws://localhost:8765" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="topic" label="Topic">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deviceType" label="设备类型">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="authType" label="鉴权方式">
                <Select
                  options={['session', 'gateway_aksk', 'internal'].map((value) => ({
                    label: value,
                    value,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
                <Select options={STATUS_OPTIONS} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default SensorRegistryPage;
