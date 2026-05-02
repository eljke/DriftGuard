import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": "http://localhost:8080",
      "/actuator": "http://localhost:8080",
      "/swagger-ui.html": "http://localhost:8080",
      "/v3": "http://localhost:8080"
    }
  },
  build: {
    outDir: "../../../target/classes/static",
    emptyOutDir: true,
    chunkSizeWarningLimit: 650,
    rollupOptions: {
      output: {
        manualChunks: {
          react: ["react", "react-dom", "@tanstack/react-query"],
          charts: ["echarts"],
          icons: ["lucide-react"]
        }
      }
    }
  }
});
