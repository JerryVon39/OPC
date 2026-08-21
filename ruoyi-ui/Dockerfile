# ============================================
# 数智游民创新工场 · 前端镜像
# 构建上下文 = 项目根目录（共享 docker/nginx.conf）
# 阶段1：Node 构建 dist；阶段2：Nginx 托管
# ============================================
FROM node:16-alpine AS builder
WORKDIR /build
COPY ruoyi-ui/package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY ruoyi-ui ./
RUN npm run build:prod

FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
