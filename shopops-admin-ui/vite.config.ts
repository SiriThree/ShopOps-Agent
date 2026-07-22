import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  base: "/admin/",
  build: {
    outDir: "../shopops-admin/src/main/resources/static/admin",
    emptyOutDir: false,
    rollupOptions: {
      input: {
        workbench: "workbench.html",
        reports: "reports.html"
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080"
    }
  }
});
