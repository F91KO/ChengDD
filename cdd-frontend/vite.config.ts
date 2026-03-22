import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/echarts')) {
            return 'echarts-vendor';
          }

          if (id.includes('node_modules/vue') || id.includes('node_modules/pinia')) {
            return 'vue-vendor';
          }

          if (id.includes('node_modules')) {
            return 'app-vendor';
          }

          return undefined;
        },
      },
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api/auth': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
      '/api/product': {
        target: 'http://127.0.0.1:8084',
        changeOrigin: true,
      },
      '/api/order': {
        target: 'http://127.0.0.1:8085',
        changeOrigin: true,
      },
      '/api/report': {
        target: 'http://127.0.0.1:8088',
        changeOrigin: true,
      },
      '/api/config': {
        target: 'http://127.0.0.1:8089',
        changeOrigin: true,
      },
    },
  },
});
