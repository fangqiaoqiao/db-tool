# 本地先执行 mvn package -DskipTests，再 docker build
# ARM 示例: docker build --platform linux/arm64 -t db-tools:arm64 .
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/db-tools-1.0.0.jar app.jar

EXPOSE 8080

# 配置文件写入 java.io.tmpdir/db-tools/db-config.json。
# 容器启动时先清理旧配置；应用进程启动时也会再次清理，保证“重启后丢失、运行期内可保存”。
ENTRYPOINT ["sh", "-c", "rm -rf /tmp/db-tools && exec java -jar /app/app.jar"]
