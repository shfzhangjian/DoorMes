import type { Plugin, PreviewServer, ViteDevServer } from "vite";
import { FileFactoryStore } from "../factory-api/src/store.js";
import { createFactoryHttpHandler } from "../factory-api/src/http.js";

/** Mount the real filesystem-backed workflow service under the web origin. */
export function createFactoryApiPlugin(directory: string, password?: string): Plugin {
  const mount = (server: ViteDevServer | PreviewServer) => {
    const handler = createFactoryHttpHandler(new FileFactoryStore(directory, password));
    server.middlewares.use((req, res, next) => { void handler(req, res, next); });
  };
  return {
    name: "doormes-factory-workflow-api",
    configureServer(server) { server.config.logger.info(`DoorMes factory JSON: ${directory}`); mount(server); },
    configurePreviewServer(server) { mount(server); }
  };
}
