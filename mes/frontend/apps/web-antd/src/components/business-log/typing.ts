export type BusinessLogMode = 'audit' | 'operation';

export interface BusinessLogItem {
  actionCode?: string;
  actionName?: string;
  businessSnapshot?: Record<string, any>;
  fromNodeName?: string;
  fromStatus?: string;
  handleTime?: string;
  handlerUserName?: string;
  id?: number;
  opinion?: string;
  toNodeName?: string;
  toStatus?: string;
}
