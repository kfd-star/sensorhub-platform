import {
  getInterfaceInfoByIdUsingGET,
  invokeInterfaceInfoUsingPOST,
} from '@/services/sensorhub-backend/interfaceInfoController';
import { PageContainer } from '@ant-design/pro-components';
import { Button, Card, Descriptions, Divider, Form, Input, message } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { useParams } from '@@/exports';

const InterfaceInfoPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<API.InterfaceInfo>();
  const [invokeRes, setInvokeRes] = useState<any>();
  const [invokeLoading, setInvokeLoading] = useState(false);
  const params = useParams();

  const loadData = useCallback(async () => {
    if (!params.id) {
      message.error('缺少接口编号');
      return;
    }
    setLoading(true);
    try {
      const res = await getInterfaceInfoByIdUsingGET({
        id: Number(params.id),
      });
      setData(res.data);
    } catch (error: any) {
      message.error(`加载接口详情失败：${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const onFinish = async (values: any) => {
    if (!params.id) {
      message.error('缺少接口编号');
      return;
    }
    setInvokeLoading(true);
    try {
      const res = await invokeInterfaceInfoUsingPOST({
        id: params.id,
        ...values,
      });
      setInvokeRes(res.data);
      message.success('调用成功');
    } catch (error: any) {
      message.error(`调用失败：${error.message}`);
    } finally {
      setInvokeLoading(false);
    }
  };

  return (
    <PageContainer title="接口详情" loading={loading}>
      <Card>
        {data ? (
          <Descriptions title={data.name} column={1}>
            <Descriptions.Item label="状态">{data.status ? '已上线' : '未上线'}</Descriptions.Item>
            <Descriptions.Item label="描述">{data.description}</Descriptions.Item>
            <Descriptions.Item label="请求地址">{data.url}</Descriptions.Item>
            <Descriptions.Item label="请求方式">{data.method}</Descriptions.Item>
            <Descriptions.Item label="请求参数">{data.requestParams}</Descriptions.Item>
            <Descriptions.Item label="请求头">{data.requestHeader}</Descriptions.Item>
            <Descriptions.Item label="响应头">{data.responseHeader}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{data.createTime}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{data.updateTime}</Descriptions.Item>
          </Descriptions>
        ) : (
          <>未找到对应接口</>
        )}
      </Card>
      <Divider />
      <Card title="在线调试">
        <Form name="invoke" layout="vertical" onFinish={onFinish}>
          <Form.Item label="请求参数 JSON" name="userRequestParams">
            <Input.TextArea rows={8} placeholder='{"limit":20,"device_token":"sensor-device-001"}' />
          </Form.Item>
          <Form.Item wrapperCol={{ span: 16 }}>
            <Button type="primary" htmlType="submit">
              调用接口
            </Button>
          </Form.Item>
        </Form>
      </Card>
      <Divider />
      <Card title="调用结果" loading={invokeLoading}>
        <pre
          style={{
            margin: 0,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
          }}
        >
          {typeof invokeRes === 'string' ? invokeRes : JSON.stringify(invokeRes, null, 2)}
        </pre>
      </Card>
    </PageContainer>
  );
};

export default InterfaceInfoPage;
