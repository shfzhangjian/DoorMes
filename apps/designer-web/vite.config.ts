import { isAbsolute, resolve } from "node:path";
import { fileURLToPath, URL } from "node:url";
import { defineConfig, loadEnv } from "vite";
import { createLocalVisualAssetApiPlugin } from "./vite-visual-asset-api.js";
import { createFactoryApiPlugin } from "./vite-factory-api.js";

/**
 * Configures the browser entry used to exercise the shared core and both
 * layout shells during the migration.
 *
 * The output remains isolated under the app folder. Production deployment
 * settings will be added only after parity and backend requirements are fixed.
 *
 * @example Run `npm run dev` at the repository root.
 * @since 0.1.0
 * @modified 2026-09-17 - Added the initial designer-web Vite configuration.
 * @modified 2026-09-18 - Added the project-local visual-asset upload API.
 */
export default defineConfig(({ mode }) => {
  const appDirectory = fileURLToPath(new URL(".", import.meta.url));
  const repositoryDirectory = fileURLToPath(new URL("../..", import.meta.url));
  const environment = loadEnv(mode, repositoryDirectory, "");
  const configuredStorageDirectory =
    environment.DOORMES_VISUAL_ASSET_DIRECTORY?.trim() || "runtime-data/visual-assets";
  const storageDirectory = isAbsolute(configuredStorageDirectory)
    ? configuredStorageDirectory
    : resolve(repositoryDirectory, configuredStorageDirectory);

  return {
    root: appDirectory,
    envDir: repositoryDirectory,
    plugins: [
      createFactoryApiPlugin(resolve(repositoryDirectory, environment.DOORMES_FACTORY_DIRECTORY?.trim() || "runtime-data/factory"), environment.DOORMES_PROTOTYPE_PASSWORD),
      createLocalVisualAssetApiPlugin({
        storageDirectory,
        actorId: environment.DOORMES_DEV_ACTOR_ID?.trim() || "local-worker"
      })
    ],
    server: {
      host: "0.0.0.0"
    },
    build: {
      outDir: "dist",
      emptyOutDir: true
    }
  };
});
