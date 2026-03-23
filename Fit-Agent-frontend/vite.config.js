import path from "path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  base: "./",
  server: {
    host: "127.0.0.1",
    port: 5500,
    strictPort: true,
  },
  build: {
    outDir: "dist",
    rollupOptions: {
      input: {
        fitAgentVite: path.resolve(__dirname, "fitagent-vite.html"),
      },
    },
  },
});
