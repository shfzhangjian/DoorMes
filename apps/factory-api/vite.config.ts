import { defineConfig } from "vite";
import { fileURLToPath } from "node:url";

export default defineConfig({
  build: {
    ssr: fileURLToPath(new URL("./src/server.ts", import.meta.url)),
    outDir: fileURLToPath(new URL("./dist", import.meta.url)),
    emptyOutDir: true,
    rollupOptions: { output: { entryFileNames: "server.js" } }
  },
  ssr: { noExternal: /^@doormes\//u }
});
