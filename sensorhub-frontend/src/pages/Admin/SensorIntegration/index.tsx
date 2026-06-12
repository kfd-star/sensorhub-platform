import {
  getSensorWorkspaceCatalogUsingGET,
  syncSensorWorkspaceCatalogUsingPOST,
} from '@/services/sensorhub-backend/sensorWorkspaceController';
import { PageContainer } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import { Alert, Button, Card, message, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useEffect, useState } from 'react';

type CatalogItem = {
  name?: string;
  description?: string;
  method?: string;
  gatewayPath?: string;
  platformUrl?: string;
  targetPath?: string;
  requestParams?: string;
  synced?: boolean;
  interfaceInfoId?: number;
  status?: number;
};

type SyncResult = {
  total?: number;
  createdCount?: number;
  updatedCount?: number;
};

const SensorWorkspaceCatalogPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [catalog, setCatalog] = useState<CatalogItem[]>([]);

  const loadCatalog = async () => {
    setLoading(true);
    try {
      const res = await getSensorWorkspaceCatalogUsingGET();
      setCatalog(res?.data || []);
    } catch (error: any) {
      message.error(`加载目录失败：${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const res = await syncSensorWorkspaceCatalogUsingPOST();
      const result: SyncResult = res?.data || {};
      message.success(
        `同步完成：共 ${result.total || 0} 个，新增 ${result.createdCount || 0} 个，更新 ${result.updatedCount || 0} 个`,
      );
      await loadCatalog();
    } catch (error: any) {
      message.error(`同步失败：${error.message}`);
    } finally {
      setSyncing(false);
    }
  };

  useEffect(() => {
    void loadCatalog();
  }, []);

  const columns: ColumnsType<CatalogItem> = [
    { title: '名称', dataIndex: 'name', key: 'name', width: 220 },
    {
      title: '请求方式',
      dataIndex: 'method',
      key: 'method',
      width: 100,
      render: (value) => <Tag color="blue">{value}</Tag>,
    },
    {
      title: 'Gateway 路径',
      dataIndex: 'gatewayPath',
      key: 'gatewayPath',
      width: 260,
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
      title: '平台状态',
      dataIndex: 'synced',
      key: 'synced',
      width: 150,
      render: (_, record) => {
        if (!record.synced) {
          return <Tag>未同步</Tag>;
        }
        if (record.status === 1) {
          return <Tag color="green">已上线</Tag>;
        }
        return <Tag color="orange">已同步 / 未上线</Tag>;
      },
    },
    {
      title: '接口 ID',
      dataIndex: 'interfaceInfoId',
      key: 'interfaceInfoId',
      width: 120,
      render: (value) => value || '-',
    },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  ];

  return (
    <PageContainer title="目录发布中心">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="SensorHub 北向接口目录"
          description="这里统一维护本地传感器接口目录，包括 GET 查询接口和 POST 写入接口；同步后会自动更新平台接口中心。"
        />
        <Card>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text>
              传感器接口由 `sensorhub-interface` 统一代理，并通过 SensorHub 接口中心发布。
            </Typography.Text>
            <Space>
              <Button onClick={() => void loadCatalog()} loading={loading}>
                刷新
              </Button>
              <Button type="primary" onClick={() => void handleSync()} loading={syncing}>
                同步到接口中心
              </Button>
              <Button onClick={() => history.push('/admin/interface_info')}>打开接口中心</Button>
              <Button onClick={() => history.push('/admin/sensor_registry')}>打开注册中心</Button>
            </Space>
          </Space>
        </Card>
        <Card title="北向接口目录">
          <Table<CatalogItem>
            rowKey={(record) => record.gatewayPath || record.name || 'catalog-item'}
            loading={loading}
            columns={columns}
            dataSource={catalog}
            pagination={false}
            scroll={{ x: 1200 }}
          />
        </Card>
      </Space>
    </PageContainer>
  );
};

export default SensorWorkspaceCatalogPage;
