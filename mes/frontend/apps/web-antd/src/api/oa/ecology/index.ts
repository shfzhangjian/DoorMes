import { requestClient } from '#/api/request';

export namespace OaEcologyApi {
  export interface ConfigStatus {
    enabled: boolean;
    origin?: string;
    openApiOrigin?: string;
    appKeyConfigured: boolean;
    appSecretConfigured: boolean;
    corpIdConfigured: boolean;
    accessTokenConfigured: boolean;
    senderTenantKey?: string;
    senderEmployeeId?: string;
    senderName?: string;
    eventId?: number;
    moduleId?: number;
    channels?: number[];
  }

  export interface User {
    employeeId?: string;
    userId?: string;
    username?: string;
    tenantKey?: string;
    workCode?: string;
    mobile?: string;
    email?: string;
    status?: string;
  }

  export interface SendMessageReq {
    title: string;
    text: string;
    receiverEmployeeId?: string;
    receiverTenantKey?: string;
    receiverWorkCode?: string;
    receiverName?: string;
    pcUrl?: string;
    h5Url?: string;
    entityId?: string;
    entityName?: string;
    todo?: boolean;
    channels?: number[];
    senderEmployeeId?: string;
    senderTenantKey?: string;
    senderName?: string;
    senderWorkCode?: string;
    eventId?: number;
    moduleId?: number;
  }

  export interface SendMessageResp {
    success: boolean;
    messageId?: string;
    code?: string;
    message?: string;
    httpStatus?: number;
    resolvedReceiver?: User;
    rawResponse?: string;
  }
}

export function getOaEcologyConfigStatus() {
  return requestClient.get<OaEcologyApi.ConfigStatus>(
    '/oa/ecology/config-status',
  );
}

export function sendOaEcologyTestMessage(data: OaEcologyApi.SendMessageReq) {
  return requestClient.post<OaEcologyApi.SendMessageResp>(
    '/oa/ecology/messages/send-test',
    data,
  );
}
