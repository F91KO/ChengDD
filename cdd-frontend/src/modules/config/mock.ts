export const configGroups = [
  {
    name: 'gateway-local.yaml',
    description: '网关路由、鉴权白名单、超时与转发策略。',
    status: '已发布',
    statusTone: 'success',
  },
  {
    name: 'merchant-service-local.yaml',
    description: '商户中心、库存同步、消息通知模板与补偿开关。',
    status: '待审核',
    statusTone: 'primary',
  },
  {
    name: 'order-service-local.yaml',
    description: '订单超时关闭、售后规则与退款回调参数。',
    status: '草稿',
    statusTone: 'default',
  },
];

export const publishRecords = [
  {
    version: 'v1.2.3',
    operator: '林一',
    target: '全量商户',
    result: '灰度成功',
    tone: 'success',
  },
  {
    version: 'v1.2.2',
    operator: '陈沫',
    target: '测试商户组',
    result: '已回滚',
    tone: 'danger',
  },
  {
    version: 'v1.2.1',
    operator: '林一',
    target: '单商户验证',
    result: '已发布',
    tone: 'info',
  },
];
