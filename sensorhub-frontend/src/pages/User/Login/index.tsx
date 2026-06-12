import Footer from '@/components/Footer';
import { userLoginUsingPOST } from '@/services/sensorhub-backend/userController';
import { getFakeCaptcha } from '@/services/sensorhub-portal/login';
import {
  AlipayCircleOutlined,
  LockOutlined,
  MobileOutlined,
  TaobaoCircleOutlined,
  UserOutlined,
  WeiboCircleOutlined,
} from '@ant-design/icons';
import {
  LoginForm,
  ProFormCaptcha,
  ProFormCheckbox,
  ProFormText,
} from '@ant-design/pro-components';
import { history, useModel } from '@umijs/max';
import { Alert, message, Tabs } from 'antd';
import React, { useState } from 'react';
import styles from './index.less';

const LoginMessage: React.FC<{ content: string }> = ({ content }) => (
  <Alert style={{ marginBottom: 24 }} message={content} type="error" showIcon />
);

const Login: React.FC = () => {
  const [userLoginState, setUserLoginState] = useState<API.LoginResult>({});
  const [type, setType] = useState<string>('account');
  const { setInitialState } = useModel('@@initialState');

  const handleSubmit = async (values: API.UserLoginRequest) => {
    try {
      const res = await userLoginUsingPOST({
        ...values,
      });
      if (res.data) {
        setUserLoginState({});
        await setInitialState((state) => ({
          ...state,
          loginUser: res.data,
        }));
        const urlParams = new URL(window.location.href).searchParams;
        history.push(urlParams.get('redirect') || '/');
        message.success('登录成功');
        return;
      }
      setUserLoginState({
        status: 'error',
        type,
      });
      message.error('登录失败，请检查账号信息');
    } catch (error) {
      setUserLoginState({
        status: 'error',
        type,
      });
      message.error('登录失败，请稍后重试');
    }
  };

  const { status, type: loginType } = userLoginState;

  return (
    <div className={styles.container}>
      <div className={styles.content}>
        <LoginForm
          logo={<img alt="logo" src="/logo.svg" />}
          title="SensorHub"
          subTitle="统一接口与传感数据平台"
          initialValues={{ autoLogin: true }}
          actions={[
            '其他登录方式：',
            <AlipayCircleOutlined key="alipay" className={styles.icon} />,
            <TaobaoCircleOutlined key="taobao" className={styles.icon} />,
            <WeiboCircleOutlined key="weibo" className={styles.icon} />,
          ]}
          onFinish={async (values) => {
            await handleSubmit(values as API.UserLoginRequest);
          }}
        >
          <Tabs
            activeKey={type}
            onChange={setType}
            centered
            items={[
              { key: 'account', label: '账号密码登录' },
              { key: 'mobile', label: '手机号登录' },
            ]}
          />

          {status === 'error' && loginType === 'account' && (
            <LoginMessage content="账号或密码错误，请重试。" />
          )}

          {type === 'account' && (
            <>
              <ProFormText
                name="userAccount"
                fieldProps={{
                  size: 'large',
                  prefix: <UserOutlined className={styles.prefixIcon} />,
                }}
                placeholder="请输入账号"
                rules={[{ required: true, message: '请输入账号' }]}
              />
              <ProFormText.Password
                name="userPassword"
                fieldProps={{
                  size: 'large',
                  prefix: <LockOutlined className={styles.prefixIcon} />,
                }}
                placeholder="请输入密码"
                rules={[{ required: true, message: '请输入密码' }]}
              />
            </>
          )}

          {status === 'error' && loginType === 'mobile' && (
            <LoginMessage content="验证码错误，请重试。" />
          )}

          {type === 'mobile' && (
            <>
              <ProFormText
                fieldProps={{
                  size: 'large',
                  prefix: <MobileOutlined className={styles.prefixIcon} />,
                }}
                name="mobile"
                placeholder="请输入手机号"
                rules={[
                  { required: true, message: '请输入手机号' },
                  { pattern: /^1\d{10}$/, message: '手机号格式不正确' },
                ]}
              />
              <ProFormCaptcha
                fieldProps={{
                  size: 'large',
                  prefix: <LockOutlined className={styles.prefixIcon} />,
                }}
                captchaProps={{ size: 'large' }}
                placeholder="请输入验证码"
                captchaTextRender={(timing, count) => {
                  if (timing) {
                    return `${count} 秒后重新获取`;
                  }
                  return '获取验证码';
                }}
                name="captcha"
                rules={[{ required: true, message: '请输入验证码' }]}
                onGetCaptcha={async (phone) => {
                  const result = await getFakeCaptcha({ phone });
                  if (result === false) {
                    return;
                  }
                  message.success('验证码发送成功，测试验证码为 1234');
                }}
              />
            </>
          )}

          <div style={{ marginBottom: 24 }}>
            <ProFormCheckbox noStyle name="autoLogin">
              自动登录
            </ProFormCheckbox>
            <a style={{ float: 'right' }}>忘记密码？</a>
          </div>
        </LoginForm>
      </div>
      <Footer />
    </div>
  );
};

export default Login;
