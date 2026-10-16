# 本地先执行 mvn package -DskipTests，再 docker build
# ARM 示例: docker build --platform linux/arm64 -t db-tools:arm64 .
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/db-tools-1.0.0.jar app.jar

EXPOSE 8080

# 数据库为页面内固定的两个磐维库，不读取外部配置文件
ENTRYPOINT ["java", "-jar", "app.jar"]
