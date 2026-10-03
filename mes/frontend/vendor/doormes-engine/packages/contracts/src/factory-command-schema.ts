import { FACTORY_ROLES } from "@doormes/contracts/factory-workflow";

/** JSON-serializable schema: the same contract is served by /api/factory/schema. */
const string = { type: "string", minLength: 1, maxLength: 2000 };
const id = { type: "string", minLength: 1, maxLength: 200 };
const ids = { type: "array", items: id, uniqueItems: true, maxItems: 10000 };
const demand = {
  type: "object", additionalProperties: false,
  required: ["widthMm", "heightMm", "material", "glass", "hardware", "finish", "dueDate", "note"],
  properties: { widthMm: { type: "number", exclusiveMinimum: 0, maximum: 50000 }, heightMm: { type: "number", exclusiveMinimum: 0, maximum: 50000 }, material: id, glass: id, hardware: id, finish: id, dueDate: { type: "string", maxLength: 40 }, note: { type: "string", maxLength: 2000 } }
};
const fields: Record<string, unknown> = {
  designId: id, releaseId: id, orderId: id, lineId: id, packageId: id, changeId: id, schemeId: id, deviationId: id, requestId: id, assigneeId: id, recordId: id,
  name: { ...id }, drawingNumber: { ...id, maxLength: 100 }, number: { ...id, maxLength: 100 }, customer: id,
  productCode: { ...id, maxLength: 100 }, batch: { ...id, maxLength: 100 },
  document: { type: "object", required: ["schemaVersion", "designId", "revision", "windows"], properties: { schemaVersion: { const: "doormes-domain.v1" }, revision: { type: "integer", minimum: 0 }, windows: { type: "array" } } },
  role: { enum: FACTORY_ROLES }, decision: { enum: ["approve", "reject"] }, note: string,
  kind: { enum: ["standard", "custom", "material", "process", "inspection", "rework", "scrap"] },
  quantity: { type: "integer", minimum: 1, maximum: 1000 }, requirement: demand,
  stepId: id, stepIds: { ...ids, minItems: 1 }, pieceNumbers: ids, risk: string, control: string, result: { enum: ["pass", "fail", "recorded"] }, actual: string, reason: string,
  fromStep: id, disposition: { enum: ["use-as-is", "rework", "scrap", "replace", "isolate"] },
  scopes: { type: "array", minItems: 2, maxItems: 200, items: { type: "object", additionalProperties: false, required: ["packageId", "batch", "fromStep", "pieceNumbers", "disposition"], properties: { packageId: id, batch: { ...id, maxLength: 100 }, fromStep: id, pieceNumbers: ids, disposition: { enum: ["use-as-is", "rework", "scrap", "replace", "isolate"] } } } },
  executionIds: ids, replacementPairs: { type: "array", maxItems: 1000, items: { type: "object", additionalProperties: false, required: ["oldPiece", "newPiece"], properties: { oldPiece: id, newPiece: id } } },
  route: { type: "array", minItems: 3, maxItems: 200, items: { type: "object", additionalProperties: false, required: ["id", "name", "instruction", "sourceObjectIds"], properties: { id, name: id, instruction: string, sourceObjectIds: ids, templateId: id } } },
  procurement: string, cost: string, delivery: string, resolution: string
};
const commands: [string, string[], string[]?][] = [
  ["assign-engineering", ["requestId", "assigneeId", "note"]], ["claim-engineering", ["requestId"]],
  ["save-design", ["name", "drawingNumber", "document"], ["designId"]],
  ["fork-release", ["releaseId", "name"]], ["submit-design", ["designId"]],
  ["set-design-route", ["designId", "route"]],
  ["review-design", ["designId", "role", "decision", "note"]], ["release-design", ["designId", "productCode"]],
  ["create-order", ["number", "customer", "kind", "quantity", "requirement"], ["releaseId"]],
  ["add-order-line", ["orderId", "kind", "quantity", "requirement"], ["releaseId"]],
  ["confirm-order", ["orderId"]], ["confirm-customer", ["orderId", "lineId", "releaseId"]],
  ["revise-order-quantity", ["orderId", "lineId", "quantity", "note"]],
  ["revise-order-requirement", ["orderId", "lineId", "kind", "requirement", "note"], ["releaseId"]],
  ["create-package", ["orderId", "lineId", "batch"], ["quantity"]], ["review-package", ["packageId", "role", "decision", "note"]],
  ["cancel-package", ["packageId", "note"]],
  ["issue-package", ["packageId"]], ["acknowledge-package", ["packageId"]],
  ["record-execution", ["packageId", "stepId", "kind", "quantity", "pieceNumbers", "result", "actual"], ["deviationId"]],
  ["complete-package", ["packageId"]],
  ["create-change", ["packageId", "reason", "batch", "fromStep", "pieceNumbers", "disposition", "procurement", "cost", "delivery"]],
  ["create-change-scheme", ["name", "reason", "procurement", "cost", "delivery", "scopes"]],
  ["submit-change-scheme", ["schemeId"]], ["revise-change-scheme", ["schemeId"]], ["refresh-change-scheme-impact", ["schemeId"]],
  ["withdraw-change-scheme", ["schemeId", "note"]], ["review-change-scheme", ["schemeId", "role", "decision", "note"]], ["issue-change-scheme", ["schemeId"]],
  ["submit-change", ["changeId"]], ["revise-change", ["changeId"]], ["refresh-change-impact", ["changeId"]],
  ["withdraw-change", ["changeId", "note"]],
  ["review-change", ["changeId", "role", "decision", "note"]], ["issue-change", ["changeId"]],
  ["acknowledge-change", ["changeId"]], ["close-change", ["changeId", "note"]],
  ["record-change-disposition", ["changeId", "pieceNumbers", "disposition", "executionIds", "replacementPairs", "note"]],
  ["review-change-disposition", ["changeId", "recordId", "role", "decision", "note"]],
  ["create-deviation", ["packageId", "reason", "actual", "pieceNumbers", "stepIds", "risk", "control"]],
  ["revise-deviation", ["deviationId", "reason", "actual", "pieceNumbers", "stepIds", "risk", "control"]],
  ["withdraw-deviation", ["deviationId", "note"]],
  ["review-deviation", ["deviationId", "role", "decision", "note"]],
  ["link-deviation-change", ["deviationId", "changeId"]], ["close-deviation", ["deviationId", "resolution"]]
];
export const FACTORY_MUTATION_JSON_SCHEMA = {
  $schema: "http://json-schema.org/draft-07/schema#",
  $id: "urn:doormes:factory-command:v1",
  title: "DoorMes factory workflow mutation v1",
  type: "object", additionalProperties: false, required: ["expectedRevision", "idempotencyKey", "command"],
  properties: {
    expectedRevision: { type: "integer", minimum: 0 },
    idempotencyKey: { type: "string", minLength: 1, maxLength: 100 },
    command: { oneOf: commands.map(([type, required, optional = []]) => ({
      type: "object", additionalProperties: false, required: ["type", ...required],
      properties: { type: { const: type }, ...Object.fromEntries([...required, ...optional].map((key) => [key, fields[key]])) }
    })) }
  }
};
