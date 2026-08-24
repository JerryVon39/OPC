# ============================================
# 数智游民创新工场 · 后端镜像（多阶段构建）
# 阶段1：Maven 构建 jar；阶段2：JRE 运行
# 构建：docker build -t jerryvon/opc-backend:版本号 .
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 先复制 pom 与源码（利用 Docker 层缓存，pom 不变时不重复下载依赖）
COPY pom.xml .
COPY ruoyi-common/pom.xml ruoyi-common/
COPY ruoyi-framework/pom.xml ruoyi-framework/
COPY ruoyi-system/pom.xml ruoyi-system/
COPY ruoyi-quartz/pom.xml ruoyi-quartz/
COPY ruoyi-generator/pom.xml ruoyi-generator/
COPY ruoyi-admin/pom.xml ruoyi-admin/
RUN mvn -B dependency:go-offline -pl ruoyi-admin -am || true
COPY ruoyi-common ruoyi-common
COPY ruoyi-framework ruoyi-framework
COPY ruoyi-system ruoyi-system
COPY ruoyi-quartz ruoyi-quartz
COPY ruoyi-generator ruoyi-generator
COPY ruoyi-admin ruoyi-admin
RUN mvn -B -pl ruoyi-admin -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
# 时区与字体（验证码用 AWT 渲染，缺字体库会报错）
RUN apt-get update && apt-get install -y --no-install-recommends tzdata fontconfig \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /build/ruoyi-admin/target/ruoyi-admin.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
