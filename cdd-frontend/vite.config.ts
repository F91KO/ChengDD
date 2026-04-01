import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'happy-dom',
    include: ['src/**/*.{test,spec}.ts'],
    globals: true,
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
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/actuator/report': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
});
