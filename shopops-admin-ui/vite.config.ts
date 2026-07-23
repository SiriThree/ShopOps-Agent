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
        reports: "reports.html",
        tasks: "tasks.html",
        audit: "audit.html",
        tools: "tools.html",
        dashboard: "dashboard.html",
        prompts: "prompts.html",
        approvals: "approvals.html",
        connectors: "connectors.html",
        users: "users.html",
        auth: "auth.html"
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
