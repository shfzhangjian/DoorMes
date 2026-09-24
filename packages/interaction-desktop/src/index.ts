import type { CreateRectangularWindowCommand } from "@doormes/contracts";
import { createWindowCommandFromIntent } from "@doormes/interaction-core";

/**
 * Represents values collected by the PC form and keyboard-oriented shell.
 *
 * String dimensions mirror browser text inputs. Parsing belongs to this input
 * adapter; validation and manufacturing constraints remain in the domain.
 *
 * @example `{ widthText: "1200", heightText: "1500" }`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added first desktop input contract.
 */
export interface DesktopCreateWindowInput {
  readonly commandId: string;
  readonly windowId: string;
  readonly mark: string;
  readonly widthText: string;
  readonly heightText: string;
}

/**
 * Translates desktop form input into the shared create-window command.
 *
 * @param input Raw values from the desktop layout shell.
 * @returns The same command shape produced by the touch adapter.
 * @example Entering `1200` and `1500` creates millimetre dimensions.
 * @since 0.1.0
 * @modified 2026-09-17 - Added desktop-to-core input translation.
 */
export function desktopCreateWindowCommand(
  input: DesktopCreateWindowInput
): CreateRectangularWindowCommand {
  return createWindowCommandFromIntent({
    commandId: input.commandId,
    windowId: input.windowId,
    mark: input.mark,
    widthMm: Number(input.widthText),
    heightMm: Number(input.heightText)
  });
}
