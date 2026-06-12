import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import '@umijs/max';

const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();

  return (
    <DefaultFooter
      style={{ background: 'none' }}
      copyright={`${currentYear} SensorHub`}
      links={[
        {
          key: 'sensorhub-home',
          title: '平台首页',
          href: '',
          blankTarget: true,
        },
        {
          key: 'sensorhub-workspace',
          title: <GithubOutlined />,
          href: '/admin/sensor_workspace',
          blankTarget: true,
        },
        {
          key: 'sensorhub-registry',
          title: '注册中心',
          href: '/admin/sensor_registry',
          blankTarget: true,
        },
      ]}
    />
  );
};

export default Footer;
