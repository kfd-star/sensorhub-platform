import { PageContainer } from '@ant-design/pro-components';
import { history, useModel } from '@umijs/max';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useEffect, useMemo, useState } from 'react';
import { getSensorWorkspaceOverviewUsingGET } from '@/services/sensorhub-backend/sensorWorkspaceController';
import { getSensorDevicePreset, SENSOR_DATASET_OPTIONS } from '../constants';

type OverviewDevice = Record<string, any>;
type OverviewPayload = Record<string, any>;

const SensorOverviewPage: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const isAdmin = initialState?.loginUser?.userRole === 'admin';
  const [loading, setLoading] = useState(false);
  const [overview, setOverview] = useState<OverviewPayload>({});

  const loadOverview = async () => {
    setLoading(true);
    try {
      const response = await getSensorWorkspaceOverviewUsingGET();
      setOverview(response?.data ?? {});
    } catch (error: any) {
      message.error(error.message || '加载传感器总览失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadOverview();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const datasetRows = useMemo(() => {
    const dataStats = overview?.dataStats ?? {};
    return SENSOR_DATASET_OPTIONS.map((item) => {
      const stats = dataStats[item.statsKey] ?? {};
      return {
        key: item.key,
        dataset: item.label,
        count: stats.count ?? 0,
        earliest: stats.earliest ?? '-',
        latest: stats.latest ?? '-',
        gatewayPath: item.gatewayPath,
      };
    });
  }, [overview]);

  const deviceColumns: ColumnsType<OverviewDevice> = [
    {
      title: '设备',
      key: 'device',
      render: (_, record) => {
        const preset = getSensorDevicePreset(record.type);
        return (
          <Space>
            <Tag color="blue">{preset?.label || record.type || '-'}</Tag>
            <Typography.Text strong>{record.name || '-'}</Typography.Text>
          </Space>
        );
      },
    },
    { title: '设备编码', dataIndex: 'id', key: 'id' },
    { title: '分类', dataIndex: 'category', key: 'category' },
    {
      title: 'Token',
      dataIndex: 'token',
      key: 'token',
      render: (value) => (value ? <Typography.Text copyable>{value}</Typography.Text> : '-'),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (value) => (
        <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '停用'}</Tag>
      ),
    },
  ];

  const datasetColumns: ColumnsType<Record<string, any>> = [
    { title: '数据集', dataIndex: 'dataset', key: 'dataset' },
    { title: '记录数', dataIndex: 'count', key: 'count' },
    { title: '最早时间', dataIndex: 'earliest', key: 'earliest' },
    { title: '最新时间', dataIndex: 'latest', key: 'latest' },
    {
      title: 'Gateway 路径',
      dataIndex: 'gatewayPath',
      key: 'gatewayPath',
      render: (value) => <Typography.Text copyable>{value}</Typography.Text>,
    },
  ];

  const systemStats = overview?.systemStatus?.stats ?? {};

  return (
    <PageContainer
      title="SensorHub 工作台"
      subTitle="Phase 3: 统一门户已切到 Sensor Registry 主导模式"
      extra={[
        <Button key="refresh" onClick={() => void loadOverview()} loading={loading}>
          刷新
        </Button>,
        <Button key="devices" onClick={() => history.push('/sensor/devices')}>
          设备目录
        </Button>,
        <Button key="realtime" type="primary" onClick={() => history.push('/sensor/realtime')}>
          实时监控
        </Button>,
      ]}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          type={overview?.platformReady ? 'success' : 'warning'}
          showIcon
          message={
            overview?.platformReady
              ? 'SensorHub 工作台已就绪，门户由 SensorHub 统一承接。'
              : 'SensorHub 工作台当前存在异常，请检查后端与 MySQL 连接。'
          }
          description={
            overview?.platformError ||
            '北向接口发布、设备目录和实时通道管理已经全部纳入 SensorHub 工作台。'
          }
        />

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic
                title="已同步北向接口"
                value={overview?.catalogSynced ?? 0}
                suffix={`/ ${overview?.catalogTotal ?? 0}`}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic title="已发布北向接口" value={overview?.catalogPublished ?? 0} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic title="注册设备数" value={overview?.deviceCount ?? 0} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic title="启用设备数" value={overview?.enabledDeviceCount ?? 0} />
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic title="实时通道数" value={overview?.realtimeChannelCount ?? 0} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={18}>
            <Card loading={loading}>
              <Descriptions column={3} size="small">
                <Descriptions.Item label="平台状态">
                  <Tag color={overview?.platformReady ? 'green' : 'red'}>
                    {overview?.platformReady ? '可用' : '异常'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Gateway Base">
                  {overview?.gatewayBaseUrl ?? '-'}
                </Descriptions.Item>
                <Descriptions.Item label="Realtime WS">
                  {overview?.realtimeWsUrl ?? '-'}
                </Descriptions.Item>
                <Descriptions.Item label="数据库">
                  {overview?.health?.database ?? overview?.systemStatus?.database ?? '-'}
                </Descriptions.Item>
                <Descriptions.Item label="API Endpoints">
                  {systemStats.api_endpoints ?? '-'}
                </Descriptions.Item>
                <Descriptions.Item label="平台用户">
                  {systemStats.users ?? '-'}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={14}>
            <Card
              title="数据集概览"
              extra={
                <Space>
                  <Button type="link" onClick={() => history.push('/sensor/search')}>
                    数据查询
                  </Button>
                  <Button type="link" onClick={() => history.push('/sensor/devices')}>
                    查看设备
                  </Button>
                </Space>
              }
            >
              <Table
                rowKey="key"
                loading={loading}
                pagination={false}
                columns={datasetColumns}
                dataSource={datasetRows}
              />
            </Card>
          </Col>
          <Col xs={24} lg={10}>
            <Card title="当前集成状态" loading={loading}>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                Phase 3 目前已经把设备主数据、北向接口目录、接口绑定和实时通道纳入 Spring Boot + MySQL 注册中心。
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                当前工作台已经完全切到 SensorHub 本地链路，门户、发布、目录以及实时 WebSocket 都由当前平台统一承接。
              </Typography.Paragraph>
              <Space wrap>
                <Button onClick={() => history.push('/admin/interface_info')}>接口管理</Button>
                {isAdmin ? (
                  <Button onClick={() => history.push('/admin/sensor_registry')}>注册中心</Button>
                ) : null}
                {isAdmin ? (
                  <Button onClick={() => history.push('/admin/sensor_workspace')}>接口目录</Button>
                ) : null}
                <Button onClick={() => history.push('/sensor/realtime')}>实时工作台</Button>
              </Space>
            </Card>
          </Col>
        </Row>

        <Card
          title="设备预览"
          extra={
            <Space>
              <Button type="link" onClick={() => history.push('/sensor/devices')}>
                完整设备列表
              </Button>
              {isAdmin ? (
                <Button type="link" onClick={() => history.push('/admin/sensor_registry')}>
                  注册中心
                </Button>
              ) : null}
            </Space>
          }
        >
          {(overview?.devices ?? []).length > 0 ? (
            <Table
              rowKey="id"
              loading={loading}
              pagination={false}
              columns={deviceColumns}
              dataSource={overview?.devices ?? []}
            />
          ) : (
            <Empty description="暂无设备预览数据" />
          )}
        </Card>
      </Space>
    </PageContainer>
  );
};

export default SensorOverviewPage;
