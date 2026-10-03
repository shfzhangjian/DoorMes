import { factoryEffectivePieceNumbers, factoryReleaseScope, type FactoryWorkspace } from "@doormes/contracts/factory-workflow";
import { factoryChangeSchemeStatus } from "@doormes/contracts/factory-production-status";

/** Read-only view models resolve every label against the selected archive, not current state. */
export function describeFactoryHistory(state: FactoryWorkspace) {
  const releases = new Map(state.releases.map((entry) => [entry.id, entry]));
  const releaseLabel = (id?: string) => { const release = id ? releases.get(id) : undefined; return release ? release.drawingNumber + " V" + release.version : "未发布"; };
  return {
    revision: state.revision, savedAt: state.savedAt,
    changeSchemes: (state.changeSchemes ?? []).map((entry) => ({ ...entry, changeIds: [...entry.changeIds], status: factoryChangeSchemeStatus(state, entry.id) })),
    orders: state.orders.flatMap((order) => order.lines.map((line) => ({ id: line.id, order: order.number, mark: line.mark, kind: line.kind, quantity: line.quantity, requirementRevision: line.requirementRevision ?? 1, requirement: { ...line.requirement }, release: releaseLabel(line.releaseId), confirmed: releaseLabel(line.customerConfirmedReleaseId), status: order.status, quantityRevisions: (order.quantityRevisions ?? []).filter((entry) => entry.lineId === line.id).map((entry) => ({ ...entry })), requirementRevisions: structuredClone((order.requirementRevisions ?? []).filter((entry) => entry.lineId === line.id)) }))),
    packages: state.packages.map((entry) => ({ id: entry.id, order: state.orders.find((order) => order.id === entry.orderId)?.number ?? entry.orderId, batch: entry.batch, version: entry.version, release: releaseLabel(entry.releaseId), status: entry.status, quantity: entry.quantity, effectivePieces: factoryEffectivePieceNumbers(state, entry), originalPieces: [...entry.pieceNumbers], supersedes: entry.supersedes, cancellation: entry.cancellation ? { ...entry.cancellation } : undefined })),
    releases: state.releases.map((entry) => ({ id: entry.id, label: entry.productCode + " · " + releaseLabel(entry.id), scope: factoryReleaseScope(state, entry), at: entry.publishedAt })),
    events: [...state.audit].reverse().map((entry) => ({ ...entry }))
  };
}
/** Only released versions of one product are compared; order-specific branches are labelled. */
export function factoryComparisonCandidates(state: FactoryWorkspace, targetId: string) {
  const target = state.releases.find((entry) => entry.id === targetId);
  return target ? state.releases.filter((entry) => entry.productCode === target.productCode && entry.id !== target.id).sort((left, right) => right.version - left.version) : [];
}
