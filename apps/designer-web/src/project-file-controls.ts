import type { DesignSession } from "@doormes/application";
import {
  parseFormalDesignSnapshotText,
  serializeFormalDesignSnapshot
} from "@doormes/persistence/local-design";

const MAX_PROJECT_FILE_BYTES = 10 * 1024 * 1024;

/** Status callback supplied by the composition root's non-blocking status pill. */
export type ProjectFileStatusReporter = (
  state: "ready" | "warning" | "error",
  message: string
) => void;

/**
 * Converts a design identifier into a filesystem-safe download fragment.
 *
 * @param value Domain design ID, which may contain spaces or punctuation.
 * @returns A conservative ASCII filename fragment.
 * @example `projectFilenamePart("ORDER 1/A")` returns `ORDER-1-A`.
 * @since 0.10.21
 * @modified 2026-09-18 - Added deterministic portable project filenames.
 */
function projectFilenamePart(value: string): string {
  return value.trim().replace(/[^a-zA-Z0-9._-]+/g, "-").replace(/^-+|-+$/g, "") || "design";
}

/**
 * Mounts shared project-file actions into the active PC or mobile header.
 *
 * Algorithm: export first validates and serializes the current formal snapshot,
 * then downloads a UTF-8 JSON file. Import reads at most 10 MiB, validates the
 * entire envelope without touching current state, and only then replaces the
 * session as a new editing history. The session notification updates both
 * renderers, preview definitions, object selection and LocalStorage auto-save.
 *
 * @param header Current shell header; controls are recreated on shell changes.
 * @param session Shared PC/mobile design session.
 * @param report Non-blocking status reporter owned by the app root.
 * @returns A disposer for listeners and temporary DOM.
 * @example `mountProjectFileControls(header, session, showStorageStatus)`.
 * @since 0.10.21
 * @modified 2026-09-18 - Added cross-port save/open project actions.
 */
export function mountProjectFileControls(
  header: HTMLElement,
  session: DesignSession,
  report: ProjectFileStatusReporter
): () => void {
  const controls = document.createElement("div");
  controls.className = "project-file-controls";

  const saveButton = document.createElement("button");
  saveButton.type = "button";
  saveButton.textContent = "保存项目";
  saveButton.title = "下载当前完整设计文件";

  const openButton = document.createElement("button");
  openButton.type = "button";
  openButton.textContent = "导入项目";
  openButton.title = "打开DoorMes项目文件";

  const fileInput = document.createElement("input");
  fileInput.type = "file";
  fileInput.accept = ".doormes.json,.json,application/json";
  fileInput.hidden = true;
  fileInput.setAttribute("aria-label", "选择DoorMes项目文件");
  controls.append(saveButton, openButton, fileInput);
  header.append(controls);

  const onSave = (): void => {
    try {
      const source = serializeFormalDesignSnapshot(session.document);
      const blob = new Blob([source], { type: "application/json;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `DoorMes-${projectFilenamePart(session.document.designId)}-r${session.document.revision}.doormes.json`;
      anchor.click();
      setTimeout(() => URL.revokeObjectURL(url), 0);
      report("ready", `项目文件已生成 · r${session.document.revision}`);
    } catch (error) {
      report(
        "error",
        error instanceof Error ? `项目文件生成失败：${error.message}` : "项目文件生成失败"
      );
    }
  };

  const onChooseFile = (): void => {
    fileInput.click();
  };

  const onOpen = async (): Promise<void> => {
    const file = fileInput.files?.[0];
    // Reset immediately so selecting the same corrected file triggers change.
    fileInput.value = "";
    if (!file) return;
    if (file.size > MAX_PROJECT_FILE_BYTES) {
      report("error", "项目文件超过10 MiB限制，未修改当前设计");
      return;
    }
    report("warning", `正在校验项目文件：${file.name}`);
    try {
      const snapshot = parseFormalDesignSnapshotText(await file.text());
      session.replaceDocument(snapshot.document);
      report(
        "ready",
        `已导入${snapshot.document.windows.length}樘窗 · r${snapshot.document.revision}`
      );
    } catch (error) {
      report(
        "error",
        error instanceof Error
          ? `项目文件无效，当前设计未修改：${error.message}`
          : "项目文件无效，当前设计未修改"
      );
    }
  };

  saveButton.addEventListener("click", onSave);
  openButton.addEventListener("click", onChooseFile);
  fileInput.addEventListener("change", onOpen);

  return () => {
    saveButton.removeEventListener("click", onSave);
    openButton.removeEventListener("click", onChooseFile);
    fileInput.removeEventListener("change", onOpen);
    controls.remove();
  };
}
