# ============================================================
# 黔能碳溯 · 后端镜像（多阶段：Maven 构建 → Temurin 17 JRE 运行）
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
LABEL maintainer="qh-energy-carbon-trace platform"
WORKDIR /app
COPY --from=build /workspace/target/qnct-backend.jar /app/qnct-backend.jar
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m"
EXPOSE 8082
# 本地文件存储目录（STORAGE_TYPE=local 时使用）
VOLUME /app/data
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/qnct-backend.jar"]