import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmEvaluationTemplateApi {
  export type TemplateStatus = 'DISABLED' | 'ENABLED';
  export type VersionStatus =
    | 'APPROVED'
    | 'DRAFT'
    | 'PENDING_AUDIT'
    | 'PUBLISHED'
    | 'REJECTED';

  export interface Item {
    id?: number;
    versionId?: number;
    groupCode: string;
    groupName: string;
    groupSort: number;
    groupMaxScore: number;
    vetoOperator?: 'EQ' | 'GE' | 'GT' | 'LE' | 'LT';
    vetoScore?: number;
    vetoResult?: 'UNQUALIFIED';
    indicatorCode: string;
    indicatorName: string;
    indicatorSort: number;
    scoringRule?: string;
    maxScore: number;
    defaultDeptNames?: string;
    defaultScorerUserId?: number;
    defaultScorerUserName?: string;
    defaultScorerUserIds?: string;
    defaultScorerUserNames?: string;
    attachmentRequired?: boolean;
  }

  export interface Version {
    id?: number;
    templateId?: number;
    versionNo: string;
    status: VersionStatus;
    totalScore: number;
    qualificationScore: number;
    previousVersionId?: number;
    changeSummary?: string;
    submitterName?: string;
    submitTime?: string;
    auditorName?: string;
    auditOpinion?: string;
    auditTime?: string;
    publisherName?: string;
    publishTime?: string;
    remark?: string;
    version?: number;
    items?: Item[];
    createTime?: string;
    updateTime?: string;
  }

  export interface Log {
    id?: number;
    versionId?: number;
    action?: string;
    actionDescription?: string;
    operatorName?: string;
    createTime?: string;
  }

  export interface Template {
    id?: number;
    templateCode: string;
    templateName: string;
    sceneType: 'AUDIT' | 'PRELIMINARY' | 'QUARTER' | 'YEAR';
    materialType?: string;
    currentVersionId?: number;
    currentVersionNo?: string;
    status: TemplateStatus;
    remark?: string;
    version?: number;
    currentVersion?: Version;
    versions?: Version[];
    logs?: Log[];
    createTime?: string;
    updateTime?: string;
    /** 兼容旧的 SRM 模板引用字段，值来自真实模板与当前版本。 */
    name?: string;
    periodType?: string;
    totalScore?: number;
  }

  export interface SaveReq {
    id?: number;
    versionId?: number;
    templateCode: string;
    templateName: string;
    sceneType: string;
    materialType?: string;
    templateStatus?: string;
    templateRemark?: string;
    versionNo: string;
    totalScore: number;
    qualificationScore: number;
    changeSummary?: string;
    versionRemark?: string;
    version?: number;
    items: Item[];
  }
}

export async function getTemplatePage(
  params: PageParam & Record<string, unknown>,
) {
  const result = await requestClient.get<
    PageResult<SrmEvaluationTemplateApi.Template>
  >(
    '/mes/srm/evaluation-template/page',
    {
      params: {
        ...params,
        sceneType: params.sceneType || params.periodType,
        templateName: params.templateName || params.name,
      },
    },
  );
  return {
    ...result,
    list: (result.list || []).map(withLegacyAliases),
  };
}

export function getTemplateDetail(id: number, versionId?: number) {
  return requestClient.get<SrmEvaluationTemplateApi.Template>(
    '/mes/srm/evaluation-template/get',
    { params: { id, versionId } },
  );
}

export function getPublishedTemplateList(sceneType = 'PRELIMINARY') {
  return requestClient.get<SrmEvaluationTemplateApi.Template[]>(
    '/mes/srm/evaluation-template/published-list',
    { params: { sceneType } },
  );
}

export function createTemplate(data: SrmEvaluationTemplateApi.SaveReq) {
  return requestClient.post<number>('/mes/srm/evaluation-template/create', data);
}

export function updateTemplate(data: SrmEvaluationTemplateApi.SaveReq) {
  return requestClient.put<boolean>('/mes/srm/evaluation-template/update', data);
}

export function submitTemplateAudit(versionId: number, opinion?: string) {
  return requestClient.put<boolean>(
    '/mes/srm/evaluation-template/submit-audit',
    { versionId, opinion },
  );
}

export function auditTemplate(
  versionId: number,
  approved: boolean,
  opinion: string,
) {
  return requestClient.put<boolean>('/mes/srm/evaluation-template/audit', {
    versionId,
    approved,
    opinion,
  });
}

export function publishTemplate(versionId: number, opinion?: string) {
  return requestClient.put<boolean>('/mes/srm/evaluation-template/publish', {
    versionId,
    opinion,
  });
}

export function upgradeTemplate(versionId: number, changeSummary: string) {
  return requestClient.post<number>('/mes/srm/evaluation-template/upgrade', {
    versionId,
    changeSummary,
  });
}

function withLegacyAliases(
  template: SrmEvaluationTemplateApi.Template,
): SrmEvaluationTemplateApi.Template {
  return {
    ...template,
    name: template.templateName,
    periodType: template.sceneType,
    totalScore: template.currentVersion?.totalScore,
  };
}
