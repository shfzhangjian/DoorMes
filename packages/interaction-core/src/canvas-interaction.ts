/**
 * Semantic result of hit-testing the 2D design surface.
 *
 * The DOM/SVG adapter may discover several overlapping candidates. It converts
 * them to this renderer-neutral shape before any shell decides whether the
 * user meant selection, movement, property editing or canvas navigation.
 * Stable object IDs are optional only for canvas whitespace.
 *
 * @example A mullion rectangle resolves to `{ kind: "drag-handle", objectId: "M-1" }`.
 * @since 0.10.28
 * @modified 2026-09-20 - Added the shared 2D hit-test contract.
 */
export type CanvasInteractionHit =
  | Readonly<{
      kind: "drag-handle";
      objectId: string;
      supportsContextMenu: boolean;
    }>
  | Readonly<{
      kind: "dimension";
      objectId: string;
      dimensionKind: string;
    }>
  | Readonly<{
      kind: "object";
      objectId: string;
    }>
  | Readonly<{
      kind: "canvas";
    }>;

/**
 * Candidate IDs discovered by a renderer-specific DOM or native adapter.
 *
 * Several fields may coexist because engineering labels and narrow handles are
 * commonly nested inside wider business-object hit areas. The shared resolver
 * selects exactly one target using its documented priority.
 *
 * @example `{ dragHandleObjectId: "M-1", objectId: "CELL-1" }` resolves to `M-1`.
 * @since 0.10.28
 * @modified 2026-09-20 - Added renderer-neutral overlapping hit candidates.
 */
export interface CanvasHitCandidates {
  readonly dragHandleObjectId?: string;
  readonly dragHandleSupportsContextMenu?: boolean;
  readonly dimensionObjectId?: string;
  readonly dimensionKind?: string;
  readonly objectId?: string;
}

/**
 * User activation reduced from mouse, pen, touch or keyboard input.
 *
 * Input adapters translate device events into this small set before asking the
 * shared resolver what the action means.
 *
 * @example A desktop `dblclick` and a touch double activation both become `double`.
 * @since 0.10.28
 * @modified 2026-09-20 - Unified cross-device activation vocabulary.
 */
export type CanvasInteractionActivation =
  | "single"
  | "move"
  | "double"
  | "context";

/**
 * Renderer-neutral action requested by one resolved canvas interaction.
 *
 * `drag` and `pan` are emitted only after the shared movement threshold. The
 * UI adapter still owns presentation previews while domain changes remain in
 * commands committed on release.
 *
 * @example `{ kind: "open-properties", objectId: "SASH-1" }` is presented by the shell.
 * @since 0.10.28
 * @modified 2026-09-20 - Added the shared 2D interaction intent contract.
 */
export type CanvasInteractionIntent =
  | Readonly<{ kind: "none" }>
  | Readonly<{ kind: "select"; objectId?: string }>
  | Readonly<{ kind: "drag"; objectId: string }>
  | Readonly<{ kind: "pan" }>
  | Readonly<{ kind: "edit-dimension"; objectId: string; dimensionKind: string }>
  | Readonly<{ kind: "open-properties"; objectId: string }>
  | Readonly<{ kind: "context-menu"; objectId: string }>;

/**
 * Pixel distance required before a primary press becomes drag or pan.
 *
 * @example A 5.9px move stays pending; a 6px move activates the gesture.
 * @since 0.10.28
 * @modified 2026-09-20 - Established one threshold for desktop and mobile adapters.
 */
export const CANVAS_PRIMARY_MOVEMENT_THRESHOLD_PX = 6;

/**
 * Chooses exactly one semantic target from overlapping renderer candidates.
 *
 * Algorithm: explicit drag handles win over engineering dimensions; dimensions
 * win over ordinary objects; no candidate means canvas whitespace. This order
 * prevents a wide object hit-area from stealing a narrow handle or dimension.
 *
 * @param candidates Renderer-specific candidates at one pointer location.
 * @returns The single shared target used by every activation resolver.
 * @example A point hitting a divider and its cell resolves to the divider handle.
 * @since 0.10.28
 * @modified 2026-09-20 - Centralized 2D hit priority before ordinary sliding work.
 */
export function resolveCanvasInteractionHit(
  candidates: CanvasHitCandidates
): CanvasInteractionHit {
  if (candidates.dragHandleObjectId) {
    return {
      kind: "drag-handle",
      objectId: candidates.dragHandleObjectId,
      supportsContextMenu: candidates.dragHandleSupportsContextMenu ?? false
    };
  }
  if (candidates.dimensionObjectId && candidates.dimensionKind) {
    return {
      kind: "dimension",
      objectId: candidates.dimensionObjectId,
      dimensionKind: candidates.dimensionKind
    };
  }
  if (candidates.objectId) return { kind: "object", objectId: candidates.objectId };
  return { kind: "canvas" };
}

/**
 * Converts one semantic hit plus activation into a shell-independent intent.
 *
 * Rules:
 * - a single activation selects an object/dimension owner or clears on canvas;
 * - movement remains pending below 6px, then starts handle drag or view pan;
 * - double activation edits dimensions or opens ordinary object properties;
 * - context activation is accepted only for handles that advertise a menu.
 *
 * @param input Activation, target and total client-pixel movement since press.
 * @returns One intent; `none` means the gesture is still pending or unsupported.
 * @example Moving a divider 8px returns `drag`; moving it 2px returns `none`.
 * @since 0.10.28
 * @modified 2026-09-20 - Allowed view panning from non-draggable drawing objects.
 */
export function resolveCanvasInteractionIntent(input: Readonly<{
  readonly activation: CanvasInteractionActivation;
  readonly hit: CanvasInteractionHit;
  readonly movementPx?: number;
  readonly movementThresholdPx?: number;
}>): CanvasInteractionIntent {
  const threshold = Math.max(
    0,
    input.movementThresholdPx ?? CANVAS_PRIMARY_MOVEMENT_THRESHOLD_PX
  );
  if (input.activation === "move") {
    const distance = Number.isFinite(input.movementPx)
      ? Math.max(0, input.movementPx ?? 0)
      : 0;
    if (distance < threshold) return { kind: "none" };
    if (input.hit.kind === "drag-handle") {
      return { kind: "drag", objectId: input.hit.objectId };
    }
    // A normal object or engineering dimension retains click/double-click
    // semantics below the threshold. Beyond it, the gesture navigates the
    // drawing just like whitespace. Only explicit handles mutate geometry.
    return { kind: "pan" };
  }
  if (input.activation === "single") {
    if (input.hit.kind === "canvas") return { kind: "select" };
    return { kind: "select", objectId: input.hit.objectId };
  }
  if (input.activation === "double") {
    if (input.hit.kind === "dimension") {
      return {
        kind: "edit-dimension",
        objectId: input.hit.objectId,
        dimensionKind: input.hit.dimensionKind
      };
    }
    if (input.hit.kind === "drag-handle" || input.hit.kind === "object") {
      return { kind: "open-properties", objectId: input.hit.objectId };
    }
    return { kind: "none" };
  }
  if (
    input.activation === "context" &&
    input.hit.kind === "drag-handle" &&
    input.hit.supportsContextMenu
  ) {
    return { kind: "context-menu", objectId: input.hit.objectId };
  }
  if (input.activation === "context" && input.hit.kind === "object") {
    return { kind: "context-menu", objectId: input.hit.objectId };
  }
  return { kind: "none" };
}
