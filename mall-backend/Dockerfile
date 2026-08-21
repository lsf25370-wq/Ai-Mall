# =====================================================
# mall-backend 镜像：多阶段构建（Maven 编译 -> JRE 运行）
# =====================================================
# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
# 先拷贝 pom 与 wrapper 以利用依赖缓存
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
RUN mvn -B dependency:go-offline
# 拷贝源码并打包
COPY src src
RUN mvn -B clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# 时区
ENV TZ=Asia/Shanghai
COPY --from=builder /build/target/mall-backend-1.0.0.jar app.jar
EXPOSE 8080
# 生产 profile 通过环境变量注入 DB/Redis 等配置
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
