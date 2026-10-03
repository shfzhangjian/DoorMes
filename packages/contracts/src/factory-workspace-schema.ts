import { FACTORY_ROLES, FACTORY_SCHEMA } from "@doormes/contracts/factory-workflow";

/** Persisted/response contract. Domain drawing validation remains owned by persistence.
 * Optional legacy fields are accepted without inventing approvals or rewriting archives.
 * Cross-object references and lifecycle consistency are checked by the file service.
 */
type Schema = Record<string, unknown>;
const text: Schema = { type: "string" };
const id: Schema = { type: "string", minLength: 1 };
const number: Schema = { type: "number" };
const nonnegative: Schema = { type: "number", minimum: 0 };
const revision: Schema = { type: "integer", minimum: 0, maximum: Number.MAX_SAFE_INTEGER };
const positive: Schema = { type: "integer", minimum: 1, maximum: Number.MAX_SAFE_INTEGER };
const time: Schema = { type: "string", pattern: "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$" };
const list = (items: Schema): Schema => ({ type: "array", items });
const ids: Schema = { ...list(id), uniqueItems: true };
const choices = (...values: string[]): Schema => ({ enum: values });
const ref = (name: string): Schema => ({ $ref: "#/definitions/" + name });
const object = (properties: Record<string, Schema>, optional: string[] = []): Schema => ({
  type: "object", additionalProperties: false, properties,
  required: Object.keys(properties).filter((key) => !optional.includes(key))
});
const document: Schema = { type: "object", required: ["schemaVersion", "designId", "revision", "windows"], properties: { schemaVersion: { const: "doormes-domain.v1" }, designId: id, revision, windows: { type: "array" } } };
const disposition = choices("use-as-is", "rework", "scrap", "replace", "isolate");
const stamp = object({ note: text, actorId: id, at: time });
const scope = { oneOf: [object({ kind: { const: "catalog" } }), object({ kind: { const: "order-line" }, orderId: id, lineId: id })] };
const difference = object({ path: text, before: {}, after: {}, category: choices("geometry", "material", "bom", "route", "other") });
const definitions: Record<string, Schema> = {
  review: object({ actorId: id, role: { enum: FACTORY_ROLES }, decision: choices("approve", "reject"), note: text, at: time, revision }),
  requirement: object({ widthMm: { type: "number", exclusiveMinimum: 0 }, heightMm: { type: "number", exclusiveMinimum: 0 }, material: id, glass: id, hardware: id, finish: id, dueDate: text, note: text }),
  bom: object({ id, sourceObjectId: id, category: id, materialCode: id, name: text, sourceMark: text, grossLengthMm: nonnegative, material: text, color: text, specification: text, lengthMm: nonnegative, widthMm: nonnegative, heightMm: nonnegative, cutLeftDeg: number, cutRightDeg: number, quantity: { type: "number", exclusiveMinimum: 0 }, unit: id }),
  route: object({ id, name: id, instruction: text, sourceObjectIds: ids, templateId: id }, ["templateId"]),
  calculation: object({ fingerprint: id, catalogVersion: id, basis: { const: "reference-simulation" }, lines: list(ref("bom")), route: list(ref("route")), blockingCodes: ids, productionEligible: { const: false }, processingParameters: list({}) }),
  specification: object({
    schemaVersion: { const: "doormes-factory-product.v1" }, widthMm: nonnegative, heightMm: nonnegative, material: text, glass: text, hardware: text, finish: text,
    units: list(object({ id, mark: text, kind: choices("window", "assembly"), widthMm: nonnegative, heightMm: nonnegative, isCoplanar: { type: "boolean" } })),
    windows: list(object({ id, mark: text, widthMm: nonnegative, heightMm: nonnegative, quantity: positive, material: text, glass: text, hardware: text, finish: text }))
  }),
  design: object({ id, name: id, drawingNumber: id, version: positive, document, createdBy: id, updatedAt: time, submittedBy: id, status: choices("draft", "in-review", "returned", "approved", "released"), reviews: list(ref("review")), calculation: ref("calculation"), manufacturingRoute: list(ref("route")), baseReleaseId: id, engineeringRequestId: id, changeId: id }, ["submittedBy", "calculation", "manufacturingRoute", "baseReleaseId", "engineeringRequestId", "changeId"]),
  release: object({ id, designId: id, productCode: id, name: id, drawingNumber: id, version: positive, designVersion: positive, document, calculation: ref("calculation"), reviews: list(ref("review")), publishedBy: id, publishedAt: time, supersedes: id, usage: { const: "prototype" }, productionEligible: { const: false }, specification: ref("specification"), scope }, ["supersedes", "specification", "scope"]),
  line: object({ id, mark: id, kind: choices("standard", "custom"), quantity: positive, requirementRevision: positive, requirement: ref("requirement"), releaseId: id, engineeringRequestId: id, customerConfirmedReleaseId: id }, ["requirementRevision", "releaseId", "engineeringRequestId", "customerConfirmedReleaseId"]),
  requirementRevision: object({ lineId: id, fromRevision: positive, toRevision: positive, fromRequirementRevision: positive, toRequirementRevision: positive, before: ref("requirement"), after: ref("requirement"), fromKind: choices("standard", "custom"), toKind: choices("standard", "custom"), fromReleaseId: id, toReleaseId: id, fromRequestId: id, toRequestId: id, cancelledPackageIds: ids, differences: { ...list(difference), minItems: 1 }, note: id, actorId: id, at: time }, ["fromReleaseId", "toReleaseId", "fromRequestId", "toRequestId"]),
  order: object({ id, number: id, customer: id, revision: positive, status: choices("draft", "confirmed", "issued", "in-production", "complete"), lines: { ...list(ref("line")), minItems: 1 }, createdBy: id, updatedAt: time, quantityRevisions: list(object({ lineId: id, fromQuantity: positive, toQuantity: positive, fromRevision: positive, toRevision: positive, note: id, actorId: id, at: time, })), requirementRevisions: list(ref("requirementRevision")) }, ["quantityRevisions", "requirementRevisions"]),
  task: object({ id, orderId: id, lineId: id, requirement: ref("requirement"), designId: id, status: choices("unassigned", "assigned", "designing", "in-review", "returned", "released"), referenceReleaseId: id, assigneeId: id, assignedBy: id, assignedAt: time, claimedAt: time, requirementRevision: positive, orderRevision: positive, previousRequestId: id }, ["referenceReleaseId", "assigneeId", "assignedBy", "assignedAt", "claimedAt", "requirementRevision", "orderRevision", "previousRequestId"]),
  package: object({ id, orderId: id, orderRevision: positive, requirementRevision: positive, lineId: id, releaseId: id, version: positive, quantity: positive, batch: id, bom: list(ref("bom")), route: list(ref("route")), pieceNumbers: ids, productionEligible: { const: false }, status: choices("review", "approved", "issued", "acknowledged", "complete", "superseded", "cancelled"), reviews: list(ref("review")), createdAt: time, createdBy: id, supersedes: id, changeId: id, processingParameters: list({}), cancellation: object({ previousStatus: choices("review", "approved"), note: id, actorId: id, at: time, orderRevision: positive }) }, ["requirementRevision", "supersedes", "changeId", "cancellation"]),
  disposal: object({ id, pieceNumbers: ids, disposition, executionIds: ids, replacementPairs: list(object({ oldPiece: id, newPiece: id })), note: text, actorId: id, at: time, reviews: list(ref("review")) }),
  change: object({ id, orderId: id, lineId: id, basePackageId: id, baseReleaseId: id, reason: id, createdBy: id, createdAt: time, designId: id, schemeId: id, status: choices("designing", "impact-review", "approved", "issued", "acknowledged", "closed", "cancelled"), newReleaseId: id, newPackageId: id, differences: list(object({ path: text, before: {}, after: {}, category: choices("geometry", "material", "bom", "route", "other") })), impact: object({ executionIds: ids, procurement: text, cost: text, delivery: text }), effectivity: object({ batch: id, fromStep: id, pieceNumbers: ids }), disposition, reviews: list(ref("review")), dispositionRecords: list(ref("disposal")), customerConfirmation: object({ releaseId: id, actorId: id, at: time }), cancellation: stamp, closure: stamp }, ["schemeId", "newReleaseId", "newPackageId", "dispositionRecords", "customerConfirmation", "cancellation", "closure"]),
  changeScheme: object({ id, name: id, createdBy: id, createdAt: time, changeIds: { ...ids, minItems: 2, maxItems: 200 } }),
  deviation: object({ id, packageId: id, reason: id, actual: id, createdBy: id, createdAt: time, status: choices("requested", "returned", "authorized", "reconciling", "closed", "cancelled"), scope: object({ pieceNumbers: ids, stepIds: ids, risk: id, control: id }), reviews: list(ref("review")), executionIds: ids, changeId: id, resolution: text, closedBy: id, closedAt: time }, ["scope", "changeId", "resolution", "closedBy", "closedAt"]),
  execution: object({ id, packageId: id, orderId: id, lineId: id, batch: id, stepId: id, pieceNumbers: { ...ids, minItems: 1 }, kind: choices("material", "process", "inspection", "rework", "scrap"), quantity: positive, result: choices("pass", "fail", "recorded"), actual: id, actorId: id, at: time, deviationId: id }, ["deviationId"]),
  audit: object({ id, actorId: id, at: time, action: id, targetId: id, workspaceRevision: positive, note: text }),
  receipt: object({ key: id, actorId: id, digest: { type: "string", pattern: "^[a-f0-9]{64}$" }, targetId: id })
};
export const FACTORY_WORKSPACE_JSON_SCHEMA = {
  $schema: "http://json-schema.org/draft-07/schema#", $id: "urn:doormes:factory-workspace:v1",
  title: "DoorMes factory workspace v1", definitions,
  ...object({ schemaVersion: { const: FACTORY_SCHEMA }, organizationId: id, revision, savedAt: time, designs: list(ref("design")), releases: list(ref("release")), orders: list(ref("order")), engineeringRequests: list(ref("task")), packages: list(ref("package")), changes: list(ref("change")), changeSchemes: list(ref("changeScheme")), deviations: list(ref("deviation")), executions: list(ref("execution")), audit: list(ref("audit")), receipts: list(ref("receipt")) }, ["changeSchemes"])
};

/** Account file validation prevents ambiguous usernames/IDs and malformed role arrays. */
export const FACTORY_USERS_JSON_SCHEMA = {
  $schema: "http://json-schema.org/draft-07/schema#", $id: "urn:doormes:factory-users:v1",
  ...object({ schemaVersion: { const: "doormes-factory-users.v1" }, users: list(object({ id, name: id, organizationId: id, username: id, roles: { type: "array", minItems: 1, uniqueItems: true, items: { enum: FACTORY_ROLES } }, enabled: { type: "boolean" }, salt: { type: "string", pattern: "^[a-f0-9-]{36}$" }, passwordHash: { type: "string", pattern: "^[a-f0-9]{128}$" } })) })
};
