import type { PagedList } from '@/services/budiot/typing';

export type IotOptionValue = string | IotOption;

export interface IotOption {
  text: string;
  value: string;
}

export interface IotPagedList<T> extends PagedList<T> {}

export interface IotCategoryRecord {
  id: string;
  parentId?: string;
  path?: string;
  name: string;
  code: string;
  disabled: boolean;
  location?: number;
  hasChildren?: boolean;
  createdAt?: number;
  updatedAt?: number;
  children?: IotCategoryRecord[];
}

export interface IotVendorRecord {
  id: string;
  name: string;
  code: string;
  contactName?: string;
  contactMobile?: string;
  contactEmail?: string;
  description?: string;
  disabled: boolean;
  createdAt?: number;
  updatedAt?: number;
}

export interface IotProtocolRecord {
  id: string;
  name: string;
  code: string;
  scriptType: IotOptionValue;
  scriptContent?: string;
  scriptVersion?: number;
  description?: string;
  disabled: boolean;
  createdAt?: number;
  updatedAt?: number;
}

export interface IotProtocolMeta {
  scriptTypes: IotOption[];
  sampleScript?: string;
  sampleInputJson?: string;
}

export interface IotProtocolDebugResult {
  success: boolean;
  properties?: Array<Record<string, unknown>>;
  events?: Array<Record<string, unknown>>;
  debug?: Record<string, unknown>;
  error?: string;
  [key: string]: unknown;
}

export interface IotGatewayRecord {
  id: string;
  name: string;
  networkProtocol: IotOptionValue;
  gatewayMode: IotOptionValue;
  protocolId?: string;
  host?: string;
  port?: number;
  path?: string;
  remoteHost?: string;
  remotePort?: number;
  clientId?: string;
  username?: string;
  password?: string;
  subscribeTopic?: string;
  publishTopic?: string;
  allowAnonymous?: boolean;
  autoStart?: boolean;
  disabled: boolean;
  description?: string;
  runtimeStatus?: IotOptionValue;
  lastSeenAt?: number;
  lastError?: string;
  protocol?: {
    id?: string;
    name?: string;
  };
}

export interface IotGatewayMeta {
  networkProtocols: IotOption[];
  gatewayModes: IotOption[];
  runtimeStatuses: IotOption[];
  protocols: IotProtocolRecord[];
}

export interface IotGatewayAgentSummary {
  agentCount: number;
  claimedGatewayCount: number;
  unclaimedGatewayCount: number;
  totalGatewayCount: number;
}

export interface IotGatewayAgentGateway {
  gatewayId: string;
  nodeId?: string;
  name: string;
  protocol?: string;
  gatewayMode?: string;
  protocolId?: string;
  protocolName?: string;
  host?: string;
  port?: number;
  path?: string;
  remoteHost?: string;
  remotePort?: number;
  lastSeenAt?: number;
  status?: string;
  lastError?: string;
  claimed?: boolean;
}

export interface IotGatewayAgentRecord {
  agentId: string;
  host?: string;
  status?: string;
  lastSeenAt?: number;
  claimedGatewayCount?: number;
  gateways?: IotGatewayAgentGateway[];
}

export interface IotThingProperty {
  identifier: string;
  name: string;
  dataType?: string;
  unit?: string;
  description?: string;
}

export interface IotThingServiceParam {
  identifier: string;
  name: string;
  dataType?: string;
  value?: unknown;
  required?: boolean;
}

export interface IotThingService {
  identifier: string;
  name: string;
  callType?: string;
  inputParams?: IotThingServiceParam[];
  outputDataType?: string;
  description?: string;
}

export interface IotThingEvent {
  identifier: string;
  name: string;
  level?: string;
  outputParams?: IotThingServiceParam[];
  description?: string;
}

export interface IotProductRecord {
  id: string;
  productKey?: string;
  name: string;
  categoryId?: string;
  vendorId?: string;
  productType?: IotOptionValue;
  networkProtocol?: IotOptionValue;
  protocolId?: string;
  gatewayNodeId?: string;
  gatewayPort?: number;
  thingPropertyJson?: string;
  thingServiceJson?: string;
  thingEventJson?: string;
  description?: string;
  disabled: boolean;
  createdAt?: number;
  updatedAt?: number;
  category?: { id?: string; name?: string };
  vendor?: { id?: string; name?: string };
  protocol?: { id?: string; name?: string };
  deviceCount?: number;
  gatewayName?: string;
  gatewayStatus?: string;
}

export interface IotProductThingModelRecord {
  id: string;
  name?: string;
  productKey?: string;
  thingPropertyJson?: string;
  thingServiceJson?: string;
  thingEventJson?: string;
}

export interface IotProductMeta {
  productTypes: IotOption[];
  networkProtocols: IotOption[];
  categories: IotCategoryRecord[];
  vendors: IotVendorRecord[];
  protocols: IotProtocolRecord[];
  gatewayNodes: Array<{
    gatewayId?: string;
    nodeId?: string;
    gatewayName?: string;
    nodeName?: string;
    name?: string;
    protocol?: string;
    protocolId?: string;
    protocolName?: string;
    gatewayMode?: string;
    port?: number;
    remotePort?: number;
  }>;
}

export interface IotDeviceRecord {
  id: string;
  productId?: string;
  productKey?: string;
  productName?: string;
  deviceCode: string;
  name?: string;
  deviceName?: string;
  gatewayNodeId?: string;
  gatewayName?: string;
  imei?: string;
  iccid?: string;
  secretKey?: string;
  serialNo?: string;
  ip?: string;
  online?: boolean;
  disabled: boolean;
  description?: string;
  createdAt?: number;
  updatedAt?: number;
  lastHeartbeatAt?: number;
  lastDeviceAt?: number;
  product?: {
    id?: string;
    name?: string;
    thingPropertyJson?: string;
    thingServiceJson?: string;
    thingEventJson?: string;
  };
}

export interface IotCommandRecord {
  id: string;
  commandCode?: string;
  payloadJson?: string;
  replyRequired?: boolean;
  status?: IotOptionValue;
  deadlineAt?: number;
  createdAt?: number;
  sendAt?: number;
  replyAt?: number;
  queuedAt?: number;
  finishedAt?: number;
  responseJson?: string;
  messageTopic?: string;
  errorMessage?: string;
}

export interface IotDeviceLatestProperty {
  identifier: string;
  name?: string;
  dataType?: string;
  unit?: string;
  description?: string;
  valueJson?: string;
  deviceAt?: number;
  createdAt?: number;
}

export interface IotDeviceRawLogRecord {
  id: string;
  direction?: IotOptionValue;
  messageType?: IotOptionValue;
  protocol?: string;
  topic?: string;
  gatewayNodeId?: string;
  payload?: string;
  success?: boolean;
  deviceAt?: number;
  createdAt?: number;
  parsedJson?: string;
}

export interface IotDeviceDataLogRecord {
  id: string;
  messageId?: string;
  deviceAt?: number;
  createdAt?: number;
  properties?: Record<
    string,
    {
      identifier?: string;
      name?: string;
      unit?: string;
      valueJson?: string;
    }
  >;
}

export interface IotDeviceEventLogRecord {
  id: string;
  eventCode?: string;
  eventName?: string;
  level?: IotOptionValue;
  sourceType?: IotOptionValue;
  contentJson?: string;
  handled?: boolean;
  deviceAt?: number;
  createdAt?: number;
}

export interface IotDeviceDetail {
  device: IotDeviceRecord;
  runtime?: Record<string, unknown>;
  rawLogs?: Array<Record<string, unknown>>;
  dataLogs?: Array<Record<string, unknown>>;
  eventLogs?: Array<Record<string, unknown>>;
  pendingCommands?: IotCommandRecord[];
  commandLogs?: IotCommandRecord[];
  latestProperties?: IotDeviceLatestProperty[];
}

export interface IotDeviceMeta {
  products: IotProductRecord[];
}

export interface IotRuleCondition {
  field?: string;
  fieldName?: string;
  dataType?: string;
  operator?: string;
  value?: unknown;
}

export interface IotRuleLinkageParam {
  identifier?: string;
  name?: string;
  dataType?: string;
  value?: unknown;
}

export interface IotRuleRecord {
  id: string;
  name: string;
  code: string;
  sourceProductId?: string;
  sourceDeviceId?: string;
  triggerScene?: IotOptionValue;
  triggerIdentifier?: string;
  conditionJson?: string;
  targetType?: IotOptionValue;
  actionTitle?: string;
  actionContent?: string;
  messageChannelId?: string;
  notifyUserIdsJson?: string;
  targetUrl?: string;
  targetTopic?: string;
  linkageProductId?: string;
  linkageDeviceId?: string;
  linkageServiceIdentifier?: string;
  linkageParamsJson?: string;
  description?: string;
  disabled: boolean;
  createdAt?: number;
  updatedAt?: number;
  sourceProduct?: { id?: string; name?: string };
}

export interface IotRuleMeta {
  triggerScenes: IotOption[];
  targetTypes: IotOption[];
  products: IotProductRecord[];
  tenantUsers: Array<{
    id: string;
    name?: string;
    text?: string;
    username?: string;
    loginname?: string;
    mobile?: string;
    email?: string;
  }>;
  smsChannels: Array<{ id: string; name: string }>;
}

export interface IotRuleProductMeta {
  product?: IotProductRecord;
  devices?: IotDeviceRecord[];
  propertyFields?: IotThingProperty[];
  events?: IotThingEvent[];
  services?: IotThingService[];
}

export interface IotDashboardCard {
  title: string;
  value: number;
  subLabel?: string;
  changeRate?: number;
  rateType?: string;
  trend?: string;
  target?: string;
  rawAlerts?: number;
  ruleAlerts?: number;
}

export interface IotDashboardRatioStat {
  name: string;
  value?: number;
  deviceCount?: number;
  ratio?: number;
}

export interface IotDashboardTrend {
  rangeType?: string;
  startAt?: number;
  endAt?: number;
  startLabel?: string;
  endLabel?: string;
  labels?: string[];
  values?: number[];
  total?: number;
}

export interface IotDashboardData {
  cards: IotDashboardCard[];
  vendorStats: IotDashboardRatioStat[];
  typeStats: IotDashboardRatioStat[];
  trend: IotDashboardTrend;
}
