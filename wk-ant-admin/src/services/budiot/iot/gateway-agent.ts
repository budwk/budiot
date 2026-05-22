import { request } from '@/utils/request';
import type {
  IotGatewayAgentRecord,
  IotGatewayAgentSummary,
  IotPagedList,
} from './typing';

const API_IOT_GATEWAY_AGENT_DATA = '/platform/iot/gateway/agent/data';
const API_IOT_GATEWAY_AGENT_LIST = '/platform/iot/gateway/agent/list';

export const getIotGatewayAgentSummary = async () =>
  request<IotGatewayAgentSummary>(API_IOT_GATEWAY_AGENT_DATA, {
    method: 'GET',
  });

export const getIotGatewayAgentPage = async (params: Record<string, unknown>) =>
  request<IotPagedList<IotGatewayAgentRecord> | IotGatewayAgentRecord[]>(API_IOT_GATEWAY_AGENT_LIST, {
    method: 'POST',
    params,
  });
