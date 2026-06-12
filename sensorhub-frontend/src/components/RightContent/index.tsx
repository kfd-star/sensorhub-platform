import { QuestionCircleOutlined } from '@ant-design/icons';
import { useModel } from '@umijs/max';
import { Space } from 'antd';
import React from 'react';
import HeaderSearch from '../HeaderSearch';
import Avatar from './AvatarDropdown';
import styles from './index.less';

export type SiderTheme = 'light' | 'dark';

const GlobalHeaderRight: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  if (!initialState || !initialState.settings) {
    return null;
  }

  const { navTheme, layout } = initialState.settings;
  let className = styles.right;
  if ((navTheme === 'realDark' && layout === 'top') || layout === 'mix') {
    className = `${styles.right} ${styles.dark}`;
  }

  return (
    <Space className={className}>
      <HeaderSearch
        className={`${styles.action} ${styles.search}`}
        placeholder="搜索 SensorHub 页面"
        defaultValue="SensorHub"
        options={[
          {
            label: <a href="">SensorHub 首页</a>,
            value: 'SensorHub 首页',
          },
          {
            label: <a href="/sensor/overview">SensorHub Workspace</a>,
            value: 'SensorHub Workspace',
          },
          {
            label: <a href="/admin/interface_info">接口中心</a>,
            value: '接口中心',
          },
          {
            label: <a href="/admin/sensor_registry">注册中心</a>,
            value: '注册中心',
          },
        ]}
      />
      <span
        className={styles.action}
        onClick={() => {
          window.open('/admin/sensor_workspace', '_blank');
        }}
      >
        <QuestionCircleOutlined />
      </span>
      <Avatar />
    </Space>
  );
};

export default GlobalHeaderRight;
