import { createServer } from "node:http";
import { resolve } from "node:path";
import { FileFactoryStore } from "./store.js";
import { createFactoryHttpHandler } from "./http.js";

const directory = resolve(process.env.DOORMES_FACTORY_DIRECTORY || "runtime-data/factory");
const handler = createFactoryHttpHandler(new FileFactoryStore(directory, process.env.DOORMES_PROTOTYPE_PASSWORD));
const port = Number(process.env.DOORMES_API_PORT || 5191);
createServer((request, response) => { void handler(request, response); }).listen(port, process.env.DOORMES_API_HOST || "127.0.0.1", () => {
  console.info(`DoorMes factory API: http://127.0.0.1:${port}/api/factory/health\nJSON directory: ${directory}`);
});
