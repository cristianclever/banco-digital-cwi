# --- Etapa 1: Compilação Local no Container ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia as configurações e baixa dependências para o cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código fonte e realiza o build pulando os testes 
COPY src ./src
RUN mvn clean package -DskipTests

# --- Etapa 2: Imagem de Execução Otimizada da AWS ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copia o JAR gerado no estágio anterior
COPY --from=build /app/target/digital-bank-0.0.1-SNAPSHOT.jar app.jar

# Configurações de otimização da JVM para containers
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV APP_OUTBOX_PROCESSING_DELAY=1000

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]