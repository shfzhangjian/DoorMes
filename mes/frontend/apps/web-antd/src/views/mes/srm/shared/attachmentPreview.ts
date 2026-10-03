export type SrmPreviewableAttachment = {
  fileName?: string;
  fileType?: string;
  fileUrl?: string;
};

export type SrmAttachmentPreviewKind = 'image' | 'pdf' | 'unsupported';

const imageExtensions = new Set([
  'bmp',
  'gif',
  'jpeg',
  'jpg',
  'png',
  'svg',
  'webp',
]);

export function canPreviewSrmAttachment(
  file?: SrmPreviewableAttachment,
): boolean {
  return getSrmAttachmentPreviewKind(file) !== 'unsupported';
}

export function getSrmAttachmentPreviewKind(
  file?: SrmPreviewableAttachment,
): SrmAttachmentPreviewKind {
  if (!file?.fileUrl) {
    return 'unsupported';
  }
  const fileType = normalizeFileType(file.fileType);
  if (fileType === 'pdf' || fileType === 'application/pdf') {
    return 'pdf';
  }
  if (fileType.startsWith('image/')) {
    return 'image';
  }
  if (imageExtensions.has(fileType)) {
    return 'image';
  }
  const extension = getSrmAttachmentExtension(file.fileName || file.fileUrl);
  if (extension === 'pdf') {
    return 'pdf';
  }
  if (imageExtensions.has(extension)) {
    return 'image';
  }
  return 'unsupported';
}

export function getSrmAttachmentDisplayName(file?: SrmPreviewableAttachment) {
  return file?.fileName || getNameFromUrl(file?.fileUrl) || '附件';
}

export function getSrmPdfPreviewUrl(url?: string) {
  if (!url) {
    return '';
  }
  return `${url}${url.includes('#') ? '&' : '#'}toolbar=1&navpanes=0&scrollbar=1`;
}

function normalizeFileType(fileType?: string) {
  return String(fileType || '')
    .trim()
    .toLowerCase();
}

function getSrmAttachmentExtension(value?: string) {
  const cleanValue = String(value || '').split(/[?#]/)[0] || '';
  const index = cleanValue.lastIndexOf('.');
  return index === -1 ? '' : cleanValue.slice(index + 1).toLowerCase();
}

function getNameFromUrl(url?: string) {
  const cleanUrl = String(url || '').split(/[?#]/)[0] || '';
  const slashIndex = cleanUrl.lastIndexOf('/');
  const rawName = slashIndex === -1 ? cleanUrl : cleanUrl.slice(slashIndex + 1);
  try {
    return decodeURIComponent(rawName);
  } catch {
    return rawName;
  }
}
