import { resolve } from 'path';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

const TARGET = process.env['MOTIF_SERVER'] ?? 'http://localhost:8080';

export default defineConfig({
  base: '/app/',
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
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
