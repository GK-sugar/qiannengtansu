import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import os from 'node:os'

/** 取本机可被手机访问的局域网 IPv4（跳过虚拟机 / 回环网卡） */
function lanAddress(): string {
  const skip = /vmware|virtualbox|vethernet|loopback|mumu|memu|hyper-v|docker|tailscale|wsl/i
  const candidates: string[] = []
  for (const [name, list] of Object.entries(os.networkInterfaces())) {
    if (skip.test(name)) continue
    for (const a of list ?? []) if (a.family === 'IPv4' && !a.internal) candidates.push(a.address)
  }
  return candidates.find((ip) => ip.startsWith('192.168.')) || candidates.find((ip) => ip.startsWith('10.')) || candidates[0] || ''
}

/** 开发环境暴露 /__lan，供前端把 127.0.0.1 的验真/打卡二维码替换为局域网地址 */
const lanEndpoint: Plugin = {
  name: 'qn-lan-endpoint',
  configureServer(server) {
    server.middlewares.use('/__lan', (_req, res) => {
      res.setHeader('Content-Type', 'application/json')
      res.end(JSON.stringify({ ip: lanAddress(), port: 5175 }))
    })
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue(), lanEndpoint],
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