# 构建阶段
FROM maven:3.8.4-openjdk-11 AS builder

# 设置工作目录
WORKDIR /build

COPY . .

# 下载依赖
RUN mvn dependency:go-offline


# 构建项目
RUN mvn package -DskipTests

# 列出构建目录内容以进行调试
RUN ls -la /build
RUN find /build -name "*.jar"
RUN ls -la /build/target || echo "/build/target does not exist"

# 运行阶段
FROM openjdk:11.0-jre-buster

# 设置工作目录
WORKDIR /thrive

# 将构建的 JAR 包复制到工作目录
COPY --from=builder /build/blog/target/*.jar /thrive/blog.jar

# 暴露容器端口号
EXPOSE 9003

# 启动应用
ENTRYPOINT ["java", "-jar", "blog.jar"]
