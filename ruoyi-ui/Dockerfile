# ============================================
# 数智游民创新工场 · 前端镜像
# 构建上下文 = 项目根目录（共享 docker/nginx.conf）
# 阶段1：Node 构建 dist；阶段2：Nginx 托管
# ============================================
FROM node:18-alpine AS builder
WORKDIR /build
COPY ruoyi-ui/package*.json ./
# U-4 fix: 镜像源可覆盖（海外/内网构建用 docker build --build-arg NPM_REGISTRY=https://registry.npmjs.org）
ARG NPM_REGISTRY=https://registry.npmmirror.com
RUN npm install --registry=$NPM_REGISTRY
COPY ruoyi-ui ./
RUN npm run build:prod

FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
