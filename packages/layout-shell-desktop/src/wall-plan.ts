import { createRectangularWindowCommand, nextAvailableWindowSequence,
  type DesignSession } from "@doormes/application";
import type { DesignObjectId, WallPlan, WallPlanOpening, WallPlanSegment } from "@doormes/contracts";

const emptyPlan = (): WallPlan => ({ segments: [], openings: [] });
const id = (value: string): DesignObjectId => value as DesignObjectId;
const format = (value: number): string => Number(value.toFixed(1)).toString();
const escapeText = (value: string): string => value.replace(/[&<>"']/g, (character) => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
})[character] ?? character);

function nextId(prefix: string, plan: WallPlan): DesignObjectId {
  const taken = new Set([...plan.segments, ...plan.openings].map((item) => item.objectId));
  let index = 1;
  while (taken.has(id(`${prefix}-${index}`))) index += 1;
  return id(`${prefix}-${index}`);
}

function wallLength(wall: WallPlanSegment): number {
  return Math.hypot(wall.endMm.x - wall.startMm.x, wall.endMm.y - wall.startMm.y);
}

/** Parametric 2D site plan. Product geometry stays in the existing drawing workspace. */
export function mountWallPlanWorkspace(root: HTMLElement, session: DesignSession): () => void {
  root.classList.add("wall-plan-workspace");
  root.innerHTML = `
    <div class="wall-plan-workspace__intro">
      <strong>墙体平面</strong>
      <span>可选场景绘制 · 墙段 → 洞口 → 放入已有门窗产品</span>
    </div>
    <div class="wall-plan-workspace__body">
      <div class="wall-plan-workspace__canvas">
        <svg data-wall-scene role="img" aria-label="墙体与洞口平面图"></svg>
        <small>点击图中墙段或洞口可选中；所有数值单位为 mm。</small>
      </div>
      <div class="wall-plan-workspace__controls">
        <form data-add-wall>
          <h3>绘制墙段</h3>
          <label>接续位置<select name="anchor" data-wall-anchor><option value="">新墙体起点</option></select></label>
          <div class="wall-plan-workspace__fields">
            <label>起点 X<input name="startX" type="number" value="0" step="1"></label>
            <label>起点 Y<input name="startY" type="number" value="0" step="1"></label>
          </div>
          <div class="wall-plan-workspace__fields">
            <label>长度<input name="length" type="number" value="3000" min="300" step="1" required></label>
            <label>方向 / 转角 °<input name="angle" type="number" value="0" step="1" required></label>
          </div>
          <div class="wall-plan-workspace__fields">
            <label>墙厚<input name="thickness" type="number" value="200" min="50" max="1000" required></label>
            <label>墙高<input name="height" type="number" value="3000" min="500" max="10000" required></label>
          </div>
          <button type="submit">添加墙段</button>
        </form>
        <form data-add-opening>
          <h3>设置洞口</h3>
          <label>所在墙段<select name="wall" data-opening-wall required></select></label>
          <div class="wall-plan-workspace__fields">
            <label>类型<select name="kind"><option value="window">窗洞</option><option value="door">门洞</option></select></label>
            <label>沿墙起距<input name="offset" type="number" value="500" min="0" step="1" required></label>
          </div>
          <div class="wall-plan-workspace__fields">
            <label>洞宽<input name="width" type="number" value="1200" min="100" step="1" required></label>
            <label>洞高<input name="height" type="number" value="1500" min="100" step="1" required></label>
          </div>
          <label>窗台高（门洞填 0）<input name="sill" type="number" value="900" min="0" step="1" required></label>
          <button type="submit">添加洞口</button>
        </form>
        <form data-place-window>
          <h3>放入已有独立窗</h3>
          <label>窗洞<select name="opening" data-placement-opening required></select></label>
          <label>窗产品<select name="window" data-placement-window><option value="">空洞 / 移除放置</option></select></label>
          <button type="submit">应用放置</button>
          <label>新建窗单侧预留（mm）<input name="clearance" type="number" value="10" min="0" max="100" step="1"></label>
          <button type="button" data-create-in-opening>按洞口新建固定窗并放入</button>
          <small>单樘独立窗且数量为 1；洞口不会自动修改产品尺寸。门产品和组合窗整体放置后续接入。</small>
        </form>
        <div class="wall-plan-workspace__selection">
          <strong data-wall-selection>未选择墙段或洞口</strong>
          <button type="button" data-remove-wall-object disabled>删除所选</button>
        </div>
        <output data-wall-status aria-live="polite"></output>
      </div>
    </div>`;
  const scene = root.querySelector<SVGSVGElement>("[data-wall-scene]")!;
  const addWall = root.querySelector<HTMLFormElement>("[data-add-wall]")!;
  const addOpening = root.querySelector<HTMLFormElement>("[data-add-opening]")!;
  const placeWindow = root.querySelector<HTMLFormElement>("[data-place-window]")!;
  const wallAnchor = root.querySelector<HTMLSelectElement>("[data-wall-anchor]")!;
  const openingWall = root.querySelector<HTMLSelectElement>("[data-opening-wall]")!;
  const placementOpening = root.querySelector<HTMLSelectElement>("[data-placement-opening]")!;
  const placementWindow = root.querySelector<HTMLSelectElement>("[data-placement-window]")!;
  const selectionLabel = root.querySelector<HTMLElement>("[data-wall-selection]")!;
  const removeButton = root.querySelector<HTMLButtonElement>("[data-remove-wall-object]")!;
  const createInOpeningButton = root.querySelector<HTMLButtonElement>("[data-create-in-opening]")!;
  const status = root.querySelector<HTMLOutputElement>("[data-wall-status]")!;
  let selectedId: DesignObjectId | undefined;
  let serial = 0;
  const currentPlan = (): WallPlan => session.document.wallPlan ?? emptyPlan();
  const commit = (plan: WallPlan, message: string): void => {
    try {
      session.execute({ type: "wall-plan.update", commandId: `CMD-WALL-PLAN-${Date.now()}-${++serial}`, wallPlan: plan });
      status.value = message;
      status.dataset.state = "success";
    } catch (error) {
      status.value = error instanceof Error ? error.message : "墙体平面更新失败。";
      status.dataset.state = "error";
    }
  };
  const options = (select: HTMLSelectElement, items: Array<{ value: string; label: string }>,
    empty?: { value: string; label: string }): void => {
    const previous = select.value;
    select.replaceChildren();
    if (empty) select.add(new Option(empty.label, empty.value));
    for (const item of items) select.add(new Option(item.label, item.value));
    if ([...select.options].some((item) => item.value === previous)) select.value = previous;
  };
  const render = (): void => {
    const plan = currentPlan();
    const allPoints = plan.segments.flatMap((wall) => [wall.startMm, wall.endMm]);
    const xs = allPoints.map((point) => point.x);
    const ys = allPoints.map((point) => point.y);
    const minX = Math.min(0, ...xs);
    const maxX = Math.max(2500, ...xs);
    const minY = Math.min(0, ...ys);
    const maxY = Math.max(1500, ...ys);
    const span = Math.max(maxX - minX, maxY - minY);
    const pad = Math.max(350, span * 0.12);
    const font = Math.max(90, span * 0.045);
    scene.setAttribute("viewBox", `${minX - pad} ${minY - pad} ${maxX - minX + 2 * pad} ${maxY - minY + 2 * pad}`);
    const parts: string[] = [`<rect x="${minX - pad}" y="${minY - pad}" width="${maxX - minX + 2 * pad}" height="${maxY - minY + 2 * pad}" fill="#f8fafc"/>`];
    for (const wall of plan.segments) {
      const length = wallLength(wall);
      const ux = (wall.endMm.x - wall.startMm.x) / length;
      const uy = (wall.endMm.y - wall.startMm.y) / length;
      const intervals = (plan.openings.filter((opening) => opening.wallId === wall.objectId)
        .sort((a, b) => a.offsetMm - b.offsetMm));
      let cursor = 0;
      const solid = (start: number, end: number): void => {
        if (end <= start) return;
        parts.push(`<line x1="${wall.startMm.x + ux * start}" y1="${wall.startMm.y + uy * start}" x2="${wall.startMm.x + ux * end}" y2="${wall.startMm.y + uy * end}" stroke="#64748b" stroke-width="${wall.thicknessMm}" stroke-linecap="butt"/>`);
      };
      for (const opening of intervals) {
        solid(cursor, opening.offsetMm);
        cursor = opening.offsetMm + opening.widthMm;
      }
      solid(cursor, length);
      const midX = (wall.startMm.x + wall.endMm.x) / 2;
      const midY = (wall.startMm.y + wall.endMm.y) / 2;
      parts.push(`<line data-wall-id="${escapeText(wall.objectId)}" x1="${wall.startMm.x}" y1="${wall.startMm.y}" x2="${wall.endMm.x}" y2="${wall.endMm.y}" stroke="transparent" stroke-width="${Math.max(wall.thicknessMm, 100)}" style="cursor:pointer"/>`);
      parts.push(`<circle cx="${wall.endMm.x}" cy="${wall.endMm.y}" r="${font * 0.13}" fill="#475569"/>`);
      parts.push(`<text x="${midX - uy * (wall.thicknessMm / 2 + font * 0.7)}" y="${midY + ux * (wall.thicknessMm / 2 + font * 0.7)}" font-size="${font}" text-anchor="middle" fill="#233d57">${escapeText(wall.objectId)} · ${format(length)}</text>`);
      if (selectedId === wall.objectId) parts.push(`<line x1="${wall.startMm.x}" y1="${wall.startMm.y}" x2="${wall.endMm.x}" y2="${wall.endMm.y}" stroke="#0b75db" stroke-width="${Math.max(8, span * 0.005)}" pointer-events="none"/>`);
    }
    for (const opening of plan.openings) {
      const wall = plan.segments.find((item) => item.objectId === opening.wallId)!;
      const length = wallLength(wall);
      const ux = (wall.endMm.x - wall.startMm.x) / length;
      const uy = (wall.endMm.y - wall.startMm.y) / length;
      const startX = wall.startMm.x + ux * opening.offsetMm;
      const startY = wall.startMm.y + uy * opening.offsetMm;
      const endX = startX + ux * opening.widthMm;
      const endY = startY + uy * opening.widthMm;
      const placedWindow = session.document.windows.find((window) => window.objectId === opening.placedWindowId);
      const color = placedWindow ? "#0b75db" : opening.kind === "door" ? "#d97706" : "#0891b2";
      const jamb = Math.max(8, span * 0.004);
      for (const [x, y] of [[startX, startY], [endX, endY]] as const) {
        parts.push(`<line x1="${x - uy * wall.thicknessMm / 2}" y1="${y + ux * wall.thicknessMm / 2}" x2="${x + uy * wall.thicknessMm / 2}" y2="${y - ux * wall.thicknessMm / 2}" stroke="#475569" stroke-width="${jamb}" pointer-events="none"/>`);
      }
      if (placedWindow) {
        const inset = (opening.widthMm - placedWindow.widthMm) / 2;
        const aX = startX + ux * inset;
        const aY = startY + uy * inset;
        const bX = endX - ux * inset;
        const bY = endY - uy * inset;
        for (const side of [-1, 1]) {
          const depth = wall.thicknessMm * 0.22 * side;
          parts.push(`<line x1="${aX - uy * depth}" y1="${aY + ux * depth}" x2="${bX - uy * depth}" y2="${bY + ux * depth}" stroke="#315f87" stroke-width="${jamb}" pointer-events="none"/>`);
        }
        parts.push(`<line x1="${aX}" y1="${aY}" x2="${bX}" y2="${bY}" stroke="#3ba4df" stroke-width="${jamb * 0.65}" pointer-events="none"/>`);
      } else {
        parts.push(`<line x1="${startX}" y1="${startY}" x2="${endX}" y2="${endY}" stroke="${color}" stroke-width="${jamb}" stroke-dasharray="${jamb * 4} ${jamb * 2}" pointer-events="none"/>`);
      }
      parts.push(`<line data-opening-id="${escapeText(opening.objectId)}" x1="${startX}" y1="${startY}" x2="${endX}" y2="${endY}" stroke="transparent" stroke-width="${Math.max(wall.thicknessMm, 100)}" style="cursor:pointer"/>`);
      const name = placedWindow
        ? placedWindow.mark
        : opening.kind === "door" ? "门洞" : "窗洞";
      parts.push(`<text x="${(startX + endX) / 2}" y="${(startY + endY) / 2 - font * 0.45}" font-size="${font * 0.85}" text-anchor="middle" fill="${color}" pointer-events="none">${escapeText(name)} ${format(opening.widthMm)}×${format(opening.heightMm)}</text>`);
      if (selectedId === opening.objectId) parts.push(`<circle cx="${(startX + endX) / 2}" cy="${(startY + endY) / 2}" r="${font * 0.2}" fill="#0b75db" pointer-events="none"/>`);
    }
    scene.innerHTML = parts.join("");
    options(wallAnchor, plan.segments.map((wall) => ({ value: wall.objectId, label: `${wall.objectId} 终点` })), { value: "", label: "新墙体起点" });
    options(openingWall, plan.segments.map((wall) => ({ value: wall.objectId, label: `${wall.objectId} · ${format(wallLength(wall))} mm` })));
    options(placementOpening, plan.openings.filter((opening) => opening.kind === "window").map((opening) => ({ value: opening.objectId, label: `${opening.objectId} · ${format(opening.widthMm)}×${format(opening.heightMm)}` })));
    const assemblyMemberIds = new Set((session.document.assemblies ?? []).flatMap((assembly) =>
      assembly.instances.map((instance) => instance.windowId)));
    options(placementWindow, session.document.windows.filter((window) =>
      window.quantity === 1 && !assemblyMemberIds.has(window.objectId)
    ).map((window) => ({ value: window.objectId, label: `${window.mark} · ${format(window.widthMm)}×${format(window.heightMm)}` })), { value: "", label: "空洞 / 移除放置" });
    if (selectedId && ![...plan.segments, ...plan.openings].some((item) => item.objectId === selectedId)) selectedId = undefined;
    const wall = plan.segments.find((item) => item.objectId === selectedId);
    const opening = plan.openings.find((item) => item.objectId === selectedId);
    selectionLabel.textContent = wall ? `${wall.objectId} · 墙长 ${format(wallLength(wall))} mm` :
      opening ? `${opening.objectId} · ${opening.kind === "door" ? "门洞" : "窗洞"} ${format(opening.widthMm)} mm` : "未选择墙段或洞口";
    removeButton.disabled = !selectedId;
    for (const input of addWall.querySelectorAll<HTMLInputElement>('[name="startX"], [name="startY"]')) input.disabled = Boolean(wallAnchor.value);
  };
  const onWallSubmit = (event: SubmitEvent): void => {
    event.preventDefault();
    const fields = new FormData(addWall);
    const plan = currentPlan();
    const anchor = plan.segments.find((wall) => wall.objectId === fields.get("anchor"));
    const startMm = anchor ? anchor.endMm : { x: Number(fields.get("startX")), y: Number(fields.get("startY")) };
    const baseDeg = anchor ? Math.atan2(anchor.endMm.y - anchor.startMm.y,
      anchor.endMm.x - anchor.startMm.x) * 180 / Math.PI : 0;
    const radians = (baseDeg + Number(fields.get("angle"))) * Math.PI / 180;
    const length = Number(fields.get("length"));
    const wall: WallPlanSegment = {
      objectId: nextId("WALL", plan),
      startMm,
      endMm: { x: startMm.x + Math.cos(radians) * length, y: startMm.y + Math.sin(radians) * length },
      thicknessMm: Number(fields.get("thickness")),
      heightMm: Number(fields.get("height")),
      ...(anchor ? { connectsToWallId: anchor.objectId } : {})
    };
    commit({ ...plan, segments: [...plan.segments, wall] }, `已添加 ${wall.objectId}。`);
    if (session.document.wallPlan?.segments.some((item) => item.objectId === wall.objectId)) {
      wallAnchor.value = wall.objectId;
      const angle = addWall.elements.namedItem("angle");
      if (angle instanceof HTMLInputElement) angle.value = "0";
      render();
    }
  };
  const onOpeningSubmit = (event: SubmitEvent): void => {
    event.preventDefault();
    const fields = new FormData(addOpening);
    const plan = currentPlan();
    const opening: WallPlanOpening = {
      objectId: nextId("OPENING", plan), wallId: id(String(fields.get("wall"))),
      kind: fields.get("kind") === "door" ? "door" : "window",
      offsetMm: Number(fields.get("offset")), widthMm: Number(fields.get("width")),
      sillMm: Number(fields.get("sill")), heightMm: Number(fields.get("height"))
    };
    commit({ ...plan, openings: [...plan.openings, opening] }, `已添加 ${opening.objectId}。`);
  };
  const onPlacementSubmit = (event: SubmitEvent): void => {
    event.preventDefault();
    const fields = new FormData(placeWindow);
    const plan = currentPlan();
    const openingId = String(fields.get("opening"));
    const windowId = String(fields.get("window"));
    if (!plan.openings.some((opening) => opening.objectId === openingId)) {
      status.value = "请先创建并选择窗洞。";
      status.dataset.state = "error";
      return;
    }
    commit({ ...plan, openings: plan.openings.map((opening) => opening.objectId === openingId
      ? { ...opening, placedWindowId: windowId ? id(windowId) : undefined } : opening) },
    windowId ? "窗产品已放入洞口。" : "已移除洞口中的窗产品。");
  };
  const onCreateInOpening = (): void => {
    const fields = new FormData(placeWindow);
    const plan = currentPlan();
    const opening = plan.openings.find((item) => item.objectId === fields.get("opening"));
    if (!opening || opening.kind !== "window" || opening.placedWindowId) {
      status.value = "请选择尚未放入产品的窗洞。";
      status.dataset.state = "error";
      return;
    }
    const clearance = Number(fields.get("clearance"));
    if (!Number.isFinite(clearance) || clearance < 0 || clearance > 100) {
      status.value = "单侧预留必须在 0–100 mm 之间。";
      status.dataset.state = "error";
      return;
    }
    let sequence = nextAvailableWindowSequence(session.document);
    const marks = new Set(session.document.windows.map((window) => window.mark.toUpperCase()));
    const windowIds = new Set(session.document.windows.map((window) => window.objectId));
    while (marks.has(`C${sequence}`) || windowIds.has(id(`WIN-${sequence}`))) sequence += 1;
    const windowId = id(`WIN-${sequence}`);
    const transactionId = `CMD-WALL-WINDOW-${Date.now()}-${++serial}`;
    try {
      session.executeTransaction([
        createRectangularWindowCommand({
          commandId: `${transactionId}:CREATE`, windowId, mark: `C${sequence}`,
          widthMm: opening.widthMm - clearance * 2,
          heightMm: opening.heightMm - clearance * 2
        }),
        {
          type: "wall-plan.update", commandId: `${transactionId}:PLACE`,
          wallPlan: { ...plan, openings: plan.openings.map((item) =>
            item.objectId === opening.objectId ? { ...item, placedWindowId: windowId } : item) }
        }
      ], transactionId);
      status.value = `已新建固定窗 C${sequence} 并放入 ${opening.objectId}；可切回门窗产品继续修改构造。`;
      status.dataset.state = "success";
    } catch (error) {
      status.value = error instanceof Error ? error.message : "创建窗失败。";
      status.dataset.state = "error";
    }
  };
  const onSceneClick = (event: MouseEvent): void => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const openingId = target.getAttribute("data-opening-id");
    const wallId = target.getAttribute("data-wall-id");
    selectedId = id(openingId ?? wallId ?? "") || undefined;
    if (openingId) placementOpening.value = openingId;
    if (wallId) openingWall.value = wallId;
    render();
  };
  const onRemove = (): void => {
    if (!selectedId) return;
    const plan = currentPlan();
    const targetId = selectedId;
    const descendants = new Set<DesignObjectId>([targetId]);
    let changed = true;
    while (changed) {
      changed = false;
      for (const wall of plan.segments) {
        if (wall.connectsToWallId && descendants.has(wall.connectsToWallId) && !descendants.has(wall.objectId)) {
          descendants.add(wall.objectId);
          changed = true;
        }
      }
    }
    const next: WallPlan = {
      segments: plan.segments.filter((wall) => !descendants.has(wall.objectId)),
      openings: plan.openings.filter((opening) => !descendants.has(opening.objectId) && !descendants.has(opening.wallId))
    };
    commit(next, `已删除 ${targetId} 及其依附对象。`);
  };
  addWall.addEventListener("submit", onWallSubmit);
  addOpening.addEventListener("submit", onOpeningSubmit);
  placeWindow.addEventListener("submit", onPlacementSubmit);
  createInOpeningButton.addEventListener("click", onCreateInOpening);
  scene.addEventListener("click", onSceneClick);
  removeButton.addEventListener("click", onRemove);
  wallAnchor.addEventListener("change", render);
  const dispose = session.subscribe(render);
  return () => {
    dispose();
    addWall.removeEventListener("submit", onWallSubmit);
    addOpening.removeEventListener("submit", onOpeningSubmit);
    placeWindow.removeEventListener("submit", onPlacementSubmit);
    createInOpeningButton.removeEventListener("click", onCreateInOpening);
    scene.removeEventListener("click", onSceneClick);
    removeButton.removeEventListener("click", onRemove);
    wallAnchor.removeEventListener("change", render);
    root.replaceChildren();
  };
}
