import { requestClient } from '#/api/request';

export namespace SrmAttachmentApi {
  export interface Attachment {
    id?: number;
    bizType?: string;
    bizId?: number;
    attachmentCategory?: string;
    fileName?: string;
    fileUrl?: string;
    fileType?: string;
    fileSize?: number;
    versionGroupNo?: string;
    versionNo?: number;
    previousAttachmentId?: number;
    latestVersion?: boolean;
    versionTime?: string;
    updateDescription?: string;
    uploadUserId?: number;
    uploadUserName?: string;
    uploadTime?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface VersionUpdate {
    sourceAttachmentId: number;
    attachmentCategory: string;
    fileName: string;
    fileUrl: string;
    fileType?: string;
    fileSize?: number;
    updateDescription: string;
  }
}

export function getAttachmentList(params: {
  bizId: number;
  bizType: string;
  includeHistory?: boolean;
}) {
  return requestClient.get<SrmAttachmentApi.Attachment[]>(
    '/mes/srm/attachment/list',
    { params },
  );
}

export function createAttachment(data: SrmAttachmentApi.Attachment) {
  return requestClient.post<number>('/mes/srm/attachment/create', data);
}

export function updateAttachmentVersion(data: SrmAttachmentApi.VersionUpdate) {
  return requestClient.post<number>('/mes/srm/attachment/version-update', data);
}

export function deleteAttachment(id: number) {
  return requestClient.delete<boolean>(`/mes/srm/attachment/delete?id=${id}`);
}
