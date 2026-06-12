import { PageContainer } from '@ant-design/pro-components';
import { useSearchParams } from '@umijs/max';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import React, { useEffect, useMemo, useState } from 'react';
import { listPublicSensorRegistryDevicesUsingGET } from '@/services/sensorhub-backend/sensorRegistryController';
import { searchSensorWorkspaceDatasetUsingGET } from '@/services/sensorhub-backend/sensorWorkspaceController';
import { getSensorDevicePreset, SENSOR_DATASET_OPTIONS } from '../constants';
import { buildDynamicColumns, buildRowKey } from '../utils';

type RegistryDevice = {
  id?: number;
  deviceCode?: string;
  deviceName?: string;
  deviceType?: string;
  category?: string;
  description?: string;
  deviceToken?: string;
  dataEndpoint?: string;
  realtimeKey?: string;
  status?: number;
};

type SearchResult = Record<string, any>;

const SensorSearchPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);
  const [deviceLoading, setDeviceLoading] = useState(false);
  const [devices, setDevices] = useState<RegistryDevice[]>([]);
  const [selectedDeviceCode, setSelectedDeviceCode] = useState<string>(
    searchParams.get('deviceCode') || '',
  );
  const [dataset, setDataset] = useState<string>(searchParams.get('dataset') || 'gps');
  const [deviceToken, setDeviceToken] = useState<string>(searchParams.get('deviceToken') || '');
  const [startDate, setStartDate] = useState<string>(searchParams.get('startDate') || '');
  const [endDate, setEndDate] = useState<string>(searchParams.get('endDate') || '');
  const [limit, setLimit] = useState<number>(Number(searchParams.get('limit')) || 20);
  const [result, setResult] = useState<SearchResult>({});

  const loadDevices = async () => {
    setDeviceLoading(true);
    try {
      const response = await listPublicSensorRegistryDevicesUsingGET();
      setDevices(response?.data ?? []);
    } catch (error: any) {
      message.error(error.message || '加载设备目录失败');
    } finally {
      setDeviceLoading(false);
    }
  };

  const runSearch = async (params: {
    dataset: string;
    deviceToken?: string;
    startDate?: string;
    endDate?: string;
    limit?: number;
  }) => {
    setLoading(true);
    try {
      const response = await searchSensorWorkspaceDatasetUsingGET(params);
      setResult(response?.data ?? {});
    } catch (error: any) {
      message.error(error.message || '查询历史数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadDevices();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const queryDataset = searchParams.get('dataset') || 'gps';
    const queryDeviceCode = searchParams.get('deviceCode') || '';
    const queryDeviceToken = searchParams.get('deviceToken') || '';
    const queryStartDate = searchParams.get('startDate') || '';
    const queryEndDate = searchParams.get('endDate') || '';
    const queryLimit = Number(searchParams.get('limit')) || 20;

    setDataset(queryDataset);
    setSelectedDeviceCode(queryDeviceCode);
    setDeviceToken(queryDeviceToken);
    setStartDate(queryStartDate);
    setEndDate(queryEndDate);
    setLimit(queryLimit);

    const hasQuery =
      !!queryDeviceCode ||
      !!queryDeviceToken ||
      !!searchParams.get('startDate') ||
      !!searchParams.get('endDate') ||
      !!searchParams.get('dataset');

    if (!hasQuery) {
      setResult({});
      return;
    }

    void runSearch({
      dataset: queryDataset,
      deviceToken: queryDeviceToken || undefined,
      startDate: queryStartDate || undefined,
      endDate: queryEndDate || undefined,
      limit: queryLimit,
    });
  }, [searchParams]); // eslint-disable-line react-hooks/exhaustive-deps

  const selectedDevice = useMemo(() => {
    if (selectedDeviceCode) {
      return devices.find((item) => item.deviceCode === selectedDeviceCode);
    }
    if (deviceToken) {
      return devices.find((item) => item.deviceToken === deviceToken);
    }
    return undefined;
  }, [deviceToken, devices, selectedDeviceCode]);

  const datasetOptions = SENSOR_DATASET_OPTIONS.map((item) => ({
    label: item.label,
    value: item.key,
  }));

  const deviceOptions = devices.map((device) => {
    const preset = getSensorDevicePreset(device.deviceType);
    return {
      label: `${device.deviceName || device.deviceCode} / ${preset?.label || device.deviceType || '-'} / ${
        device.deviceToken || '-'
      }`,
      value: device.deviceCode || '',
    };
  });

  const handleDeviceChange = (value?: string) => {
    setSelectedDeviceCode(value || '');
    const nextDevice = devices.find((item) => item.deviceCode === value);
    setDeviceToken(nextDevice?.deviceToken || '');
    const preset = getSensorDevicePreset(nextDevice?.deviceType);
    if (preset?.datasetKey) {
      setDataset(preset.datasetKey);
    }
  };

  const handleSearch = () => {
    const nextParams: Record<string, string> = {
      dataset,
      limit: String(limit || 20),
    };
    if (selectedDeviceCode) {
      nextParams.deviceCode = selectedDeviceCode;
    }
    if (deviceToken.trim()) {
      nextParams.deviceToken = deviceToken.trim();
    }
    if (startDate) {
      nextParams.startDate = startDate;
    }
    if (endDate) {
      nextParams.endDate = endDate;
    }
    setSearchParams(nextParams);
  };

  const handleReset = () => {
    setSelectedDeviceCode('');
    setDataset('gps');
    setDeviceToken('');
    setStartDate('');
    setEndDate('');
    setLimit(20);
    setResult({});
    setSearchParams({});
  };

  const columns = useMemo(() => buildDynamicColumns(result?.records ?? []), [result]);

  const selectedPreset = getSensorDevicePreset(selectedDevice?.deviceType);

  return (
    <PageContainer
      title="SensorHub 数据查询"
      subTitle="Phase 3: 设备目录与历史查询已经和 Sensor Registry 联动"
      extra={[
        <Button key="reset" onClick={handleReset}>
          重置
        </Button>,
        <Button key="search" type="primary" onClick={handleSearch}>
          查询
        </Button>,
      ]}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          showIcon
          type="info"
          message="历史查询现在由 SensorHub 工作台驱动"
          description="你可以先选设备，系统会自动带出设备 Token 和建议数据集；查询直接走 SensorHub 本地数据表和本地工作台接口。"
        />

        <Card title="查询条件">
          <Space wrap size={16}>
            <Select
              showSearch
              allowClear
              loading={deviceLoading}
              style={{ width: 420 }}
              placeholder="从注册中心选择设备"
              value={selectedDeviceCode || undefined}
              onChange={handleDeviceChange}
              options={deviceOptions}
              optionFilterProp="label"
            />
            <Select
              style={{ width: 240 }}
              value={dataset}
              onChange={setDataset}
              options={datasetOptions}
            />
            <Input
              style={{ width: 220 }}
              placeholder="设备 Token"
              value={deviceToken}
              onChange={(event) => setDeviceToken(event.target.value)}
            />
            <Input
              style={{ width: 180 }}
              type="date"
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
            />
            <Input
              style={{ width: 180 }}
              type="date"
              value={endDate}
              onChange={(event) => setEndDate(event.target.value)}
            />
            <InputNumber
              min={1}
              max={100}
              value={limit}
              onChange={(value) => setLimit(value || 20)}
            />
            <Button type="primary" onClick={handleSearch}>
              查询
            </Button>
          </Space>
        </Card>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={10}>
            <Card title="设备联动信息">
              {selectedDevice ? (
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="设备名称">
                    {selectedDevice.deviceName || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="设备编码">
                    {selectedDevice.deviceCode || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="设备类型">
                    <Tag color="blue">
                      {selectedPreset?.label || selectedDevice.deviceType || '-'}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="建议数据集">
                    {selectedPreset?.datasetKey ? (
                      <Tag color="green">{selectedPreset.datasetKey}</Tag>
                    ) : (
                      <Tag>需手动选择</Tag>
                    )}
                  </Descriptions.Item>
                  <Descriptions.Item label="设备 Token">
                    {selectedDevice.deviceToken ? (
                      <Typography.Text copyable>{selectedDevice.deviceToken}</Typography.Text>
                    ) : (
                      '-'
                    )}
                  </Descriptions.Item>
                  <Descriptions.Item label="数据接口">
                    {selectedDevice.dataEndpoint || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="实时 Key">
                    {selectedDevice.realtimeKey || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="状态">
                    <Tag color={selectedDevice.status === 1 ? 'green' : 'default'}>
                      {selectedDevice.status === 1 ? '启用' : '停用'}
                    </Tag>
                  </Descriptions.Item>
                </Descriptions>
              ) : (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="未选择注册中心设备，可直接按数据集和 Token 查询"
                />
              )}
            </Card>
          </Col>
          <Col xs={24} lg={14}>
            <Card title="查询摘要">
              <Descriptions column={2} size="small">
                <Descriptions.Item label="当前数据集">
                  <Tag color="blue">{result?.datasetName || dataset}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="返回条数">
                  {result?.count ?? 0}
                </Descriptions.Item>
                <Descriptions.Item label="Gateway 路径">
                  {result?.gatewayPath ? (
                    <Typography.Text copyable>{result.gatewayPath}</Typography.Text>
                  ) : (
                    '-'
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="设备 Token">
                  {result?.query?.device_token || deviceToken || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="开始日期">
                  {result?.query?.startDate || startDate || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="结束日期">
                  {result?.query?.endDate || endDate || '-'}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          </Col>
        </Row>

        <Card
          title="查询结果"
          extra={
            <Space>
              <Tag color="blue">{result?.datasetName ?? 'Dataset'}</Tag>
              <Typography.Text>{`Count: ${result?.count ?? 0}`}</Typography.Text>
            </Space>
          }
        >
          <Table
            rowKey={(record, index) => buildRowKey(record, index || 0)}
            loading={loading}
            scroll={{ x: 1400 }}
            columns={columns}
            dataSource={result?.records ?? []}
            locale={{ emptyText: '暂无查询结果' }}
          />
        </Card>
      </Space>
    </PageContainer>
  );
};

export default SensorSearchPage;
