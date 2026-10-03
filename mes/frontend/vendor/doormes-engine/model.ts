import { DesignSession, createRectangularWindowCommand } from '@doormes/application';
import { createEmptyDesign } from '@doormes/domain';
import { parseFormalDesignDocument } from '@doormes/persistence/local-design';
import type { DesignDocument } from '@doormes/contracts';

export function seedDocument(input: { designId:string; lineId:string; mark:string; quantity:number; widthMm:number; heightMm:number }): DesignDocument {
  const session = new DesignSession(createEmptyDesign(input.designId));
  session.execute(createRectangularWindowCommand({ commandId:`SEED-${input.lineId}`, windowId:`W-${input.lineId}`,
    mark:input.mark, quantity:input.quantity, widthMm:input.widthMm, heightMm:input.heightMm }));
  return parseFormalDesignDocument(session.document);
}
