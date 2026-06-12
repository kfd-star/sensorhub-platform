import { PageContainer } from '@ant-design/pro-components';
import { history, useModel } from '@umijs/max';
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useEffect, useMemo, useState } from 'react';
import {
  addSensorRegistryDeviceUsingPOST,
  deleteSensorRegistryDeviceUsingPOST,
  listPublicSensorRegistryDevicesUsingGET,
  listSensorRegistryDataSourcesUsingGET,
  updateSensorRegistryDeviceUsingPOST,
} from '@/services/sensorhub-backend/sensorRegistryController';
import { testSensorWorkspaceDeviceUsingPOST } from '@/services/sensorhub-backend/sensorWorkspaceController';
import { getSensorDevicePreset, SENSOR_DEVICE_PRESETS } from '../constants';
import { formatCellValue } from '../utils';

type SensorRegistryDeviceRecord = {
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
  createTime?: string;
  updateTime?: string;
};

type DeviceFormValues = {
  deviceCode: string;
  deviceName: string;
  deviceType: string;
  category?: string;
  description?: string;
  deviceToken: string;
  dataSourceId?: number;
  legacyDeviceId?: string;
  dataEndpoint?: string;
  realtimeKey?: string;
  configJson?: string;
  dataFieldsJson?: string;
  status?: number;
};

type DataSourceOption = {
  label: string;
  value: number;
};

const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const DEFAULT_CONFIG = {
  managedBy: 'SensorHub',
  stage: 'phase3',
};

const parseJson = (value?: string) => {
  if (!value) {
    return undefined;
  }
  try {
    return JSON.parse(value);
  } catch (error) {
    return value;
  }
};

const normalizeJsonString = (value: string | undefined, fallback: any) => {
  const trimmed = value?.trim();
  if (!trimmed) {
    return JSON.stringify(fallback);
  }
  return JSON.stringify(JSON.parse(trimmed));
};

const createDefaultFormValues = (dataSourceId?: number): DeviceFormValues => {
  const preset = getSensorDevicePreset('custom');
  return {
    deviceCode: '',
    deviceName: '',
    deviceType: 'custom',
    category: preset?.category,
    description: '',
    deviceToken: '',
    dataSourceId,
    legacyDeviceId: '',
    dataEndpoint: preset?.dataEndpoint,
    realtimeKey: preset?.realtimeKey,
    configJson: JSON.stringify(DEFAULT_CONFIG, null, 2),
    dataFieldsJson: JSON.stringify(preset?.dataFields || [], null, 2),
    status: 1,
  };
};

const SensorDevicesPage: React.FC = () => {
  const [form] = Form.useForm<DeviceFormValues>();
  const { initialState } = useModel('@@initialState');
  const isAdmin = initialState?.loginUser?.userRole === 'admin';

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [devices, setDevices] = useState<SensorRegistryDeviceRecord[]>([]);
  const [dataSourceOptions, setDataSourceOptions] = useState<DataSourceOption[]>([]);
  const [searchText, setSearchText] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>();
  const [statusFilter, setStatusFilter] = useState<number>();
  const [editingDevice, setEditingDevice] = useState<SensorRegistryDeviceRecord>();
  const [modalVisible, setModalVisible] = useState(false);

  const dataSourceMap = useMemo(() => {
    return new Map<number, string>(
      dataSourceOptions.map((item) => [item.value, item.label]),
    );
  }, [dataSourceOptions]);

  const loadDevices = async () => {
    setLoading(true);
    try {
      const response = await listPublicSensorRegistryDevicesUsingGET();
      setDevices(response?.data ?? []);
    } catch (error: any) {
      message.error(error.message || '加载设备目录失败');
    } finally {
      setLoading(false);
    }
  };

  const loadDataSources = async () => {
    if (!isAdmin) {
      return;
    }
    try {
      const response = await listSensorRegistryDataSourcesUsingGET();
      const options =
        response?.data?.map((item: Record<string, any>) => ({
          label: `${item.name || item.code || item.id} (${item.code || item.id})`,
          value: item.id,
        })) ?? [];
      setDataSourceOptions(options);
    } catch (error: any) {
      message.error(error.message || '加载数据源失败');
    }
  };

  useEffect(() => {
    void loadDevices();
    if (isAdmin) {
      void loadDataSources();
    }
  }, [isAdmin]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!modalVisible || editingDevice || !dataSourceOptions.length) {
      return;
    }
    if (!form.getFieldValue('dataSourceId')) {
      form.setFieldValue('dataSourceId', dataSourceOptions[0]?.value);
    }
  }, [dataSourceOptions, editingDevice, form, modalVisible]);

  const filteredDevices = useMemo(() => {
    return devices.filter((device) => {
      const keyword = searchText.trim().toLowerCase();
      const matchesSearch =
        !keyword ||
        [
          device.deviceName,
          device.deviceCode,
          device.deviceType,
          device.deviceToken,
          device.dataEndpoint,
          device.realtimeKey,
        ]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(keyword));
      const matchesType = !typeFilter || device.deviceType === typeFilter;
      const matchesStatus = statusFilter === undefined || device.status === statusFilter;
      return matchesSearch && matchesType && matchesStatus;
    });
  }, [devices, searchText, statusFilter, typeFilter]);

  const stats = useMemo(() => {
    const enabledCount = devices.filter((device) => device.status === 1).length;
    const customCount = devices.filter((device) => !getSensorDevicePreset(device.deviceType)?.datasetKey).length;
    return {
      total: devices.length,
      enabled: enabledCount,
      disabled: devices.length - enabledCount,
      custom: customCount,
      filtered: filteredDevices.length,
    };
  }, [devices, filteredDevices.length]);

  const openCreateModal = () => {
    setEditingDevice(undefined);
    form.setFieldsValue(createDefaultFormValues(dataSourceOptions[0]?.value));
    setModalVisible(true);
  };

  const openEditModal = (device: SensorRegistryDeviceRecord) => {
    setEditingDevice(device);
    form.setFieldsValue({
      deviceCode: device.deviceCode || '',
      deviceName: device.deviceName || '',
      deviceType: device.deviceType || 'custom',
      category: device.category || '',
      description: device.description || '',
      deviceToken: device.deviceToken || '',
      dataSourceId: device.dataSourceId,
      legacyDeviceId: device.legacyDeviceId || '',
      dataEndpoint: device.dataEndpoint || '',
      realtimeKey: device.realtimeKey || '',
      configJson: device.configJson
        ? JSON.stringify(parseJson(device.configJson), null, 2)
        : JSON.stringify(DEFAULT_CONFIG, null, 2),
      dataFieldsJson: device.dataFieldsJson
        ? JSON.stringify(parseJson(device.dataFieldsJson), null, 2)
        : JSON.stringify(getSensorDevicePreset(device.deviceType)?.dataFields || [], null, 2),
      status: device.status ?? 1,
    });
    setModalVisible(true);
  };

  const applyPreset = (deviceType: string) => {
    const preset = getSensorDevicePreset(deviceType);
    if (!preset) {
      return;
    }
    form.setFieldsValue({
      deviceType,
      category: preset.category,
      dataEndpoint: preset.dataEndpoint,
      realtimeKey: preset.realtimeKey,
      dataFieldsJson: JSON.stringify(preset.dataFields || [], null, 2),
    });
  };

  const buildPayload = (values: DeviceFormValues) => {
    const preset = getSensorDevicePreset(values.deviceType);
    return {
      id: editingDevice?.id,
      deviceCode: values.deviceCode.trim(),
      deviceName: values.deviceName.trim(),
      deviceType: values.deviceType,
      category: values.category?.trim() || preset?.category || '传感器',
      description: values.description?.trim() || '',
      deviceToken: values.deviceToken.trim(),
      dataSourceId: values.dataSourceId,
      legacyDeviceId: values.legacyDeviceId?.trim() || undefined,
      dataEndpoint: values.dataEndpoint?.trim() || preset?.dataEndpoint || '',
      realtimeKey: values.realtimeKey?.trim() || preset?.realtimeKey || values.deviceType,
      configJson: normalizeJsonString(values.configJson, DEFAULT_CONFIG),
      dataFieldsJson: normalizeJsonString(values.dataFieldsJson, preset?.dataFields || []),
      status: values.status ?? 1,
    };
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    let payload: Record<string, any>;
    try {
      payload = buildPayload(values);
    } catch (error: any) {
      message.error(error.message || 'JSON 字段格式不正确');
      return;
    }
    setSubmitting(true);
    try {
      if (editingDevice?.id) {
        await updateSensorRegistryDeviceUsingPOST(payload);
        message.success('设备已更新');
      } else {
        await addSensorRegistryDeviceUsingPOST(payload);
        message.success('设备已创建');
      }
      setModalVisible(false);
      setEditingDevice(undefined);
      await loadDevices();
    } catch (error: any) {
      message.error(error.message || '保存设备失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggle = async (device: SensorRegistryDeviceRecord) => {
    if (!device.id) {
      return;
    }
    setSubmitting(true);
    try {
      await updateSensorRegistryDeviceUsingPOST({
        ...device,
        status: device.status === 1 ? 0 : 1,
      });
      message.success(device.status === 1 ? '设备已停用' : '设备已启用');
      await loadDevices();
    } catch (error: any) {
      message.error(error.message || '切换设备状态失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (deviceId?: number) => {
    if (!deviceId) {
      return;
    }
    setSubmitting(true);
    try {
      await deleteSensorRegistryDeviceUsingPOST({ id: deviceId });
      message.success('设备已删除');
      await loadDevices();
    } catch (error: any) {
      message.error(error.message || '删除设备失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleTest = async (device: SensorRegistryDeviceRecord) => {
    setSubmitting(true);
    try {
      const response = await testSensorWorkspaceDeviceUsingPOST({
        name: device.deviceName,
        token: device.deviceToken,
        data_endpoint: device.dataEndpoint,
        realtime_key: device.realtimeKey,
      });
      const result = response?.data || {};
      Modal.info({
        width: 720,
        title: `${device.deviceName || device.deviceCode} 连通性测试`,
        content: (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Typography.Text>{result?.message || '测试已完成'}</Typography.Text>
            <Typography.Text type="secondary">{`目标路径: ${result?.targetPath || '-'}`}</Typography.Text>
            <Typography.Text type="secondary">{`返回数量: ${result?.count ?? 0}`}</Typography.Text>
            <Card size="small" title="样例数据">
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                {JSON.stringify(result?.sample || {}, null, 2)}
              </pre>
            </Card>
          </Space>
        ),
      });
    } catch (error: any) {
      message.error(error.message || '测试设备失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<SensorRegistryDeviceRecord> = [
    {
      title: '设备',
      key: 'device',
      width: 260,
      fixed: 'left',
      render: (_, record) => {
        const preset = getSensorDevicePreset(record.deviceType);
        return (
          <Space align="start">
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 16,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: `${preset?.color || '#1677ff'}18`,
                color: preset?.color || '#1677ff',
                fontSize: 12,
                fontWeight: 700,
              }}
            >
              {preset?.icon || '[DEV]'}
            </div>
            <div>
              <Typography.Text strong>{record.deviceName || '-'}</Typography.Text>
              <div style={{ color: 'rgba(0,0,0,0.45)', fontSize: 12, marginTop: 4 }}>
                {record.deviceCode || '-'}
              </div>
              <div style={{ color: 'rgba(0,0,0,0.65)', fontSize: 12, marginTop: 4 }}>
                {record.description || '暂无设备描述'}
              </div>
            </div>
          </Space>
        );
      },
    },
    {
      title: '类型',
      dataIndex: 'deviceType',
      key: 'deviceType',
      width: 170,
      render: (value) => {
        const preset = getSensorDevicePreset(value);
        return <Tag color="blue">{preset?.label || value || '-'}</Tag>;
      },
    },
    {
      title: '数据源',
      dataIndex: 'dataSourceId',
      key: 'dataSourceId',
      width: 180,
      render: (value) => dataSourceMap.get(value) || (value ? `#${value}` : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '停用'}</Tag>,
    },
    {
      title: 'Token',
      dataIndex: 'deviceToken',
      key: 'deviceToken',
      width: 220,
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
      title: '字段 Schema',
      dataIndex: 'dataFieldsJson',
      key: 'dataFieldsJson',
      width: 240,
      render: (value) => formatCellValue(parseJson(value)),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: isAdmin ? 340 : 120,
      fixed: 'right',
      render: (_, record) => {
        const preset = getSensorDevicePreset(record.deviceType);
        return (
          <Space wrap>
            <Button
              size="small"
              onClick={() =>
                history.push(
                  `/sensor/search?dataset=${preset?.datasetKey || 'gps'}&deviceCode=${encodeURIComponent(
                    record.deviceCode || '',
                  )}&deviceToken=${encodeURIComponent(record.deviceToken || '')}`,
                )
              }
            >
              查询
            </Button>
            {isAdmin ? (
              <>
                <Button size="small" onClick={() => handleTest(record)} loading={submitting}>
                  测试
                </Button>
                <Button size="small" type="primary" ghost onClick={() => openEditModal(record)}>
                  编辑
                </Button>
                <Button size="small" onClick={() => handleToggle(record)} loading={submitting}>
                  {record.status === 1 ? '停用' : '启用'}
                </Button>
                <Popconfirm
                  title="确认删除该设备吗？"
                  description="删除后会同步移除对应的注册中心设备记录。"
                  onConfirm={() => void handleDelete(record.id)}
                >
                  <Button size="small" danger>
                    删除
                  </Button>
                </Popconfirm>
              </>
            ) : null}
          </Space>
        );
      },
    },
  ];

  return (
    <PageContainer
      title="SensorHub 设备目录"
      subTitle="Phase 3: 设备主数据已经切到 Spring Boot Sensor Registry"
      extra={
        <Space>
          <Button onClick={() => void loadDevices()} loading={loading}>
            刷新
          </Button>
          {isAdmin ? (
            <Button type="primary" onClick={openCreateModal}>
              新建设备
            </Button>
          ) : null}
        </Space>
      }
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          showIcon
          type="info"
          message="设备目录已由 MySQL 注册中心统一管理"
          description={
            isAdmin
              ? '当前页面读取的是 Sensor Registry 中的设备主数据。新增、编辑、启停、删除会直接写入 MySQL，并同步影响派生的实时通道元数据。'
              : '你现在查看的是统一设备目录，可以直接跳转到数据查询页；设备新增、编辑和删除仍需要管理员权限。'
          }
        />

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic title="设备总数" value={stats.total} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic title="启用设备" value={stats.enabled} valueStyle={{ color: '#16a34a' }} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic title="停用设备" value={stats.disabled} valueStyle={{ color: '#ef4444' }} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic title="筛选结果" value={stats.filtered} valueStyle={{ color: '#1677ff' }} />
            </Card>
          </Col>
        </Row>

        <Card title="筛选与搜索">
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="按设备名称 / 编码 / Token / 接口搜索"
              style={{ width: 320 }}
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
            />
            <Select
              allowClear
              placeholder="筛选设备类型"
              style={{ width: 240 }}
              value={typeFilter}
              onChange={setTypeFilter}
              options={SENSOR_DEVICE_PRESETS.map((item) => ({
                label: item.label,
                value: item.type,
              }))}
            />
            <Select
              allowClear
              placeholder="筛选状态"
              style={{ width: 140 }}
              value={statusFilter}
              onChange={setStatusFilter}
              options={STATUS_OPTIONS}
            />
          </Space>
        </Card>

        <Card title={`设备列表 (${filteredDevices.length})`}>
          <Table<SensorRegistryDeviceRecord>
            rowKey={(record) => String(record.id || record.deviceCode)}
            loading={loading}
            columns={columns}
            dataSource={filteredDevices}
            scroll={{ x: 2200 }}
            locale={{
              emptyText: '暂无设备数据',
            }}
          />
        </Card>
      </Space>

      <Modal
        destroyOnClose
        open={modalVisible}
        title={editingDevice ? '编辑设备' : '新建设备'}
        onCancel={() => {
          setModalVisible(false);
          setEditingDevice(undefined);
        }}
        onOk={() => void handleSubmit()}
        confirmLoading={submitting}
        width={860}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={createDefaultFormValues(dataSourceOptions[0]?.value)}
          onValuesChange={(changedValues) => {
            if (changedValues.deviceType) {
              applyPreset(changedValues.deviceType);
            }
          }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="deviceCode"
                label="设备编码"
                rules={[{ required: true, message: '请输入设备编码' }]}
              >
                <Input placeholder="例如：sensor_gps_001" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="deviceName"
                label="设备名称"
                rules={[{ required: true, message: '请输入设备名称' }]}
              >
                <Input placeholder="例如：无人机 GPS 设备" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="deviceType"
                label="设备类型"
                rules={[{ required: true, message: '请选择设备类型' }]}
              >
                <Select
                  options={SENSOR_DEVICE_PRESETS.map((item) => ({
                    label: item.label,
                    value: item.type,
                  }))}
                />
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
                <Input placeholder="设备唯一鉴权标识" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="dataSourceId"
                label="所属数据源"
                rules={[{ required: true, message: '请选择所属数据源' }]}
              >
                <Select options={dataSourceOptions} placeholder="选择数据源" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="category" label="设备分类">
                <Input placeholder="例如：传感器" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="legacyDeviceId" label="历史设备 ID">
                <Input placeholder="保留历史系统设备标识时可填写" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="dataEndpoint"
                label="数据接口路径"
                rules={[{ required: true, message: '请输入数据接口路径' }]}
              >
                <Input placeholder="/api/gps-data" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="realtimeKey" label="实时 Key">
                <Input placeholder="gps / wind / camera" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="description" label="设备描述">
                <Input.TextArea rows={2} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="configJson" label="配置 JSON">
                <Input.TextArea rows={5} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="dataFieldsJson" label="字段 JSON">
                <Input.TextArea rows={5} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default SensorDevicesPage;
