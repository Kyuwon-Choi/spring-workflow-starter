import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

export default defineConfig({
  root: fileURLToPath(new URL('./src/main/webapp', import.meta.url)),
  plugins: [react()],
  build: {
    emptyOutDir: true,
    outDir: fileURLToPath(new URL('./target/classes/static', import.meta.url)),
  },
  server: { host: '0.0.0.0', port: 9000, proxy: { '/api': 'http://127.0.0.1:8080' } },
});
