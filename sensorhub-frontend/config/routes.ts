export default [
  { path: '/', name: 'SensorHub 首页', icon: 'smile', component: './Index' },
  {
    path: '/interface_info/:id',
    name: '接口详情',
    icon: 'smile',
    component: './InterfaceInfo',
    hideInMenu: true,
  },
  {
    path: '/user',
    layout: false,
    routes: [{ name: '登录', path: '/user/login', component: './User/Login' }],
  },
  {
    path: '/sensor',
    name: 'SensorHub 工作台',
    icon: 'dashboard',
    access: 'canUser',
    routes: [
      { path: '/sensor', redirect: '/sensor/overview' },
      { name: '总览', path: '/sensor/overview', component: './Sensor/Overview' },
      { name: '设备目录', path: '/sensor/devices', component: './Sensor/Devices' },
      { name: '实时监控', path: '/sensor/realtime', component: './Sensor/Realtime' },
      { name: '数据查询', path: '/sensor/search', component: './Sensor/Search' },
    ],
  },
  {
    path: '/admin',
    name: 'SensorHub 管理台',
    icon: 'crown',
    access: 'canAdmin',
    routes: [
      {
        name: '接口中心',
        icon: 'table',
        path: '/admin/interface_info',
        component: './Admin/InterfaceInfo',
      },
      {
        name: '调用分析',
        icon: 'analysis',
        path: '/admin/interface_analysis',
        component: './Admin/InterfaceAnalysis',
      },
      {
        path: '/admin/sensor_integration',
        redirect: '/admin/sensor_workspace',
        hideInMenu: true,
      },
      {
        name: '目录发布',
        icon: 'setting',
        path: '/admin/sensor_workspace',
        component: './Admin/SensorIntegration',
      },
      {
        name: '注册中心',
        icon: 'cluster',
        path: '/admin/sensor_registry',
        component: './Admin/SensorRegistry',
      },
    ],
  },
  { path: '*', layout: false, component: './404' },
];
