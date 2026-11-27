# ============================================================
# 黔能碳溯 · 前端镜像（多阶段：Node 构建 → Nginx 静态托管）
# 注意：nginx.conf 由 deploy/nginx.conf 挂载覆盖（含 SSE 反代配置）
# ============================================================
FROM node:20-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com || npm install --registry=https://registry.npmmirror.com
COPY . .
# 生产默认关掉 Mock；如需纯演示可 build-arg VITE_USE_MOCK=true
ARG VITE_USE_MOCK=false
ENV VITE_USE_MOCK=${VITE_USE_MOCK}
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80