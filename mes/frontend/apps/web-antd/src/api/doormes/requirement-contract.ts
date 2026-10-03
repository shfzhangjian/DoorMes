export const DEMAND_SCHEMA = 'doormes-design-demand.v1' as const;
export const REQUIREMENT_SCHEMA = 'doormes-design-requirement.v1' as const;
/** Manufacturing demand, not drawing geometry or the legacy formula snapshot. */
export interface FactoryRequirement {
  widthMm: number; heightMm: number; material: string; glass: string;
  hardware: string; finish: string; dueDate: string; note: string;
}
export interface RequirementLine {
  id?: string; mark: string; kind: 'custom' | 'standard'; quantity: number;
  requirement: FactoryRequirement;
}
export interface RequirementInput {
  schemaVersion: typeof DEMAND_SCHEMA; number: string; customer: string;
  project: string; lines: RequirementLine[]; note: string;
}
export type RequirementStatus = 'DRAFT' | 'IN_DESIGN' | 'SUBMITTED';
export interface RequirementSummary {
  id: string; number: string; customer: string; project: string; revision: number;
  status: RequirementStatus; createdBy: number; assignedTo?: number | null;
  lineCount: number; quantity: number; updatedAt: string;
}
export interface RequirementDocument {
  schemaVersion: typeof REQUIREMENT_SCHEMA; id: string; tenantId: number;
  revision: number; status: RequirementStatus; createdBy: number; assignedTo?: number | null;
  createdAt: string; updatedAt: string; changedBy: number; action: string;
  changeNote: string; demand: RequirementInput;
}
export interface RequirementVersion {
  revision: number; action: string; changeNote: string; changedBy: number; updatedAt: string;
}
