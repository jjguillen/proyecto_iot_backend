FROM docker.io/eclipse-temurin:21-jre-jammy
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

# --- LINUX ---
# chmod +x gradlew
# export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-arm64  (o en la carpeta que tengas el JDK)
# ./gradlew clean build -x test

# podman build -t springiot:1.0 .
# podman run -d --name mariadb_iot -p 3309:3306 -e MARIADB_ROOT_PASSWORD=toor -e MARIADB_DATABASE=iot -e MARIADB_USER=usuario -e MARIADB_PASSWORD=usuario1234 -v mariadb_iot_data:/var/lib/mysql mariadb:latest
# podman run -p 8080:8080 --network host  --name springiot -d -t springiot:1.0

# Pasos que habría que hacer para desplegar ----------------
# 1. Subir cambios a repositorio
# 2. En AWS EC2 descargar cambios del repositorio
# 3. Generar el fichero .jar de la app Spring
# 4. Parar y eliminar contenedor app Spring
# 5. Generar nueva imagen contenedor Spring con el nuevo .jar
# 6. Crear y lanzar el contenedor de la imagen actualizada