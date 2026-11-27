import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5175,
    strictPort: true,
    watch: {
      // 忽略 IDE 生成目录（.vs/.idea/日志），防止用 Visual Studio / IDEA 打开工程后
      // Vite 文件监听器 EBUSY 崩溃（node fs watcher locked on *.vsidx）
      ignored: ['**/.vs/**', '**/.idea/**', '**/*.vsidx', '**/*.log']
    },
    proxy: {
      // 后端接口代理：/api -> Spring Boot(8082)
      '/api': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 2500,
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ['echarts'],
          'element-plus': ['element-plus', '@element-plus/icons-vue']
        }
      }
    }
  }
})