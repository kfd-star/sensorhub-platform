import { PageContainer } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Card,
  Col,
  List,
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
import {
  getSensorWorkspaceRealtimeUsingGET,
  getSensorWorkspaceRealtimeWorkspaceUsingGET,
} from '@/services/sensorhub-backend/sensorWorkspaceController';
import DeviceStatusCard from '../components/DeviceStatusCard';
import {
  getDeviceRealtimeKey,
  getDeviceStatus,
  renderRealtimeMetrics,
} from '../realtimeRenderers';
import type { SensorDeviceRecord } from '../realtimeRenderers';
import { getSensorDevicePreset } from '../constants';
import { useSensorRealtimeSocket } from '../useSensorRealtimeSocket';

type RealtimeWorkspace = {
  wsUrl?: string;
  channels?: Record<string, any>[];
  devices?: SensorDeviceRecord[];
  channelCount?: number;
  deviceCount?: number;
  enabledDeviceCount?: number;
  generatedAt?: string;
};

const SensorRealtimePage: React.FC = () => {
  const [workspaceLoading, setWorkspaceLoading] = useState(false);
  const [snapshotLoading, setSnapshotLoading] = useState(false);
  const [workspace, setWorkspace] = useState<RealtimeWorkspace>({});
  const [snapshot, setSnapshot] = useState<Record<string, any>>({});
  const {
    connected,
    error,
    messages,
    stats,
    latestMessageMap,
    connect,
    disconnect,
    clearMessages,
    clearError,
  } = useSensorRealtimeSocket(workspace.wsUrl);

  const loadWorkspace = async () => {
    setWorkspaceLoading(true);
    try {
      const response = await getSensorWorkspaceRealtimeWorkspaceUsingGET();
      setWorkspace(response?.data || {});
    } catch (requestError: any) {
      message.error(requestError.message || '加载实时工作台失败');
    } finally {
      setWorkspaceLoading(false);
    }
  };

  const loadSnapshot = async () => {
    setSnapshotLoading(true);
    try {
      const response = await getSensorWorkspaceRealtimeUsingGET({ limit: 1 });
      setSnapshot(response?.data || {});
    } catch (requestError: any) {
      message.error(requestError.message || '加载实时快照失败');
    } finally {
      setSnapshotLoading(false);
    }
  };

  useEffect(() => {
    void loadWorkspace();
    void loadSnapshot();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (workspace.wsUrl) {
      connect();
    }
  }, [connect, workspace.wsUrl]);

  const enabledDevices = useMemo(
    () => (workspace.devices || []).filter((device) => device.enabled !== false),
    [workspace.devices],
  );

  const snapshotMap = useMemo(() => {
    const result = new Map<string, Record<string, any>>();
    (snapshot?.datasets || []).forEach((dataset: Record<string, any>) => {
      result.set(dataset.code, dataset);
    });
    return result;
  }, [snapshot]);

  const channelColumns: ColumnsType<Record<string, any>> = [
    { title: '通道编码', dataIndex: 'channelCode', key: 'channelCode', width: 220 },
    { title: '名称', dataIndex: 'name', key: 'name', width: 180 },
    { title: 'Topic', dataIndex: 'topic', key: 'topic', width: 180 },
    { title: '设备类型', dataIndex: 'deviceType', key: 'deviceType', width: 140 },
    { title: '鉴权方式', dataIndex: 'authType', key: 'authType', width: 120 },
    { title: 'WebSocket URL', dataIndex: 'wsPath', key: 'wsPath', width: 260, ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value) => (
        <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '停用'}</Tag>
      ),
    },
  ];

  return (
    <PageContainer
      title="SensorHub 实时监控"
      subTitle="Phase 3: 实时工作台已接入注册中心设备与实时通道目录"
      extra={
        <Space>
          <Button onClick={() => void loadWorkspace()} loading={workspaceLoading}>
            刷新工作台
          </Button>
          <Button onClick={() => void loadSnapshot()} loading={snapshotLoading}>
            刷新快照
          </Button>
          {connected ? (
            <Button onClick={disconnect}>断开连接</Button>
          ) : (
            <Button type="primary" onClick={connect} disabled={!workspace.wsUrl}>
              连接 WebSocket
            </Button>
          )}
          <Button danger onClick={clearMessages}>
            清空消息流
          </Button>
        </Space>
      }
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          showIcon
          type={connected ? 'success' : 'info'}
          message={connected ? '实时网关已连接' : '实时网关待连接'}
          description={
            <Space direction="vertical" size={4}>
              <Typography.Text>{`WebSocket: ${workspace.wsUrl || '未配置'}`}</Typography.Text>
              <Typography.Text type="secondary">
                当前页面直接接收 SensorHub 本地 WebSocket 实时流；如果某设备暂时没有实时消息，卡片会显示最近一次本地快照结果。
              </Typography.Text>
            </Space>
          }
        />

        {error ? (
          <Alert
            showIcon
            closable
            type="warning"
            message="实时连接提示"
            description={error}
            onClose={clearError}
          />
        ) : null}

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={workspaceLoading}>
              <Statistic title="实时消息总数" value={stats.totalMessages} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={workspaceLoading}>
              <Statistic title="活跃设备数" value={stats.activeDevices} valueStyle={{ color: '#16a34a' }} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={workspaceLoading}>
              <Statistic title="消息速率" value={stats.messageRate} suffix="/s" valueStyle={{ color: '#db2777' }} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={workspaceLoading}>
              <Statistic title="运行时长" value={stats.uptime} valueStyle={{ color: '#7c3aed' }} />
            </Card>
          </Col>
        </Row>

        <Card
          title="设备实时总览"
          extra={
            <Typography.Text type="secondary">
              {`启用设备 ${workspace.enabledDeviceCount || enabledDevices.length} / 实时通道 ${
                workspace.channelCount || 0
              } / 注册设备 ${workspace.deviceCount || 0}`}
            </Typography.Text>
          }
        >
          <Row gutter={[16, 16]}>
            {enabledDevices.length === 0 ? (
              <Col span={24}>
                <Alert
                  type="info"
                  showIcon
                  message="暂无启用设备"
                  description="请先在 Sensor Devices 或 Admin -> Sensor Registry 中启用设备。"
                />
              </Col>
            ) : (
              enabledDevices.map((device) => {
                const realtimeKey = getDeviceRealtimeKey(device);
                const preset = getSensorDevicePreset(device.type || device.deviceType);
                const latestMessage = latestMessageMap.get(realtimeKey);
                const snapshotDataset =
                  snapshotMap.get(preset?.datasetKey || '') ||
                  snapshotMap.get(realtimeKey) ||
                  snapshotMap.get(device.type) ||
                  snapshotMap.get(device.deviceType);
                const fallbackRecord = snapshotDataset?.latestRecord;
                const resolvedData = latestMessage?.data || fallbackRecord;
                const resolvedTimestamp = latestMessage?.timestamp || fallbackRecord?.timestamp;
                const status = getDeviceStatus(latestMessage?.timestamp);
                return (
                  <Col xs={24} md={12} xl={8} key={device.id}>
                    <DeviceStatusCard
                      deviceName={device.name || device.id}
                      icon={device.icon || preset?.icon}
                      color={device.color || preset?.color}
                      status={status}
                      lastUpdate={resolvedTimestamp}
                    >
                      {renderRealtimeMetrics(device, resolvedData)}
                    </DeviceStatusCard>
                  </Col>
                );
              })
            )}
          </Row>
        </Card>

        <Row gutter={[16, 16]}>
          <Col xs={24} xl={14}>
            <Card
              title={`实时消息流 (${messages.length})`}
              loading={workspaceLoading}
              extra={
                <Typography.Text type="secondary">
                  {snapshot?.generatedAt
                    ? `最近快照: ${new Date(snapshot.generatedAt).toLocaleString('zh-CN')}`
                    : '暂无快照时间'}
                </Typography.Text>
              }
            >
              <List
                locale={{ emptyText: '当前还没有收到实时消息' }}
                dataSource={messages}
                renderItem={(item) => (
                  <List.Item>
                    <List.Item.Meta
                      title={
                        <Space wrap>
                          <Tag color="blue">{item.device || 'unknown'}</Tag>
                          <Typography.Text>{item.device_name || 'Sensor Telemetry'}</Typography.Text>
                          <Typography.Text type="secondary">
                            {item.timestamp ? new Date(item.timestamp).toLocaleString('zh-CN') : '-'}
                          </Typography.Text>
                        </Space>
                      }
                      description={
                        <pre
                          style={{
                            margin: 0,
                            whiteSpace: 'pre-wrap',
                            wordBreak: 'break-all',
                            background: '#fafafa',
                            padding: 12,
                            borderRadius: 12,
                            width: '100%',
                          }}
                        >
                          {JSON.stringify(item.data || {}, null, 2)}
                        </pre>
                      }
                    />
                  </List.Item>
                )}
              />
            </Card>
          </Col>
          <Col xs={24} xl={10}>
            <Card title="实时通道目录" loading={workspaceLoading}>
              <Table<Record<string, any>>
                rowKey={(record) => record.channelCode || record.topic}
                columns={channelColumns}
                dataSource={workspace.channels || []}
                pagination={false}
                scroll={{ x: 1200, y: 520 }}
              />
            </Card>
          </Col>
        </Row>
      </Space>
    </PageContainer>
  );
};

export default SensorRealtimePage;
