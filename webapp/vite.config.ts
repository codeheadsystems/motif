import { resolve } from 'path';
import { defineConfig } from 'vite';

const TARGET = process.env['MOTIF_SERVER'] ?? 'http://localhost:8080';

export default defineConfig({
  build: {
    outDir: resolve(__dirname, '../server/src/main/resources/assets'),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    open: '/',
    proxy: {
      '/opaque': { target: TARGET, changeOrigin: true },
      '/oprf': { target: TARGET, changeOrigin: true },
      '/api': { target: TARGET, changeOrigin: true },
    },
  },
});
