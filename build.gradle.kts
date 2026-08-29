plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "faang.school"
version = "1.0"

val javaVersion = 25
val springCloudVersion = "2025.1.3"
val testcontainersVersion = "2.0.5"
val mapstructVersion = "1.6.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

dependencies {
    /**
     * Spring boot starters
     */
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.retry:spring-retry")

    /**
     * Spring Swagger
     */
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")

    /**
     * Database
     */
    implementation("redis.clients:jedis")
    runtimeOnly("org.postgresql:postgresql")

    /**
     * Amazon S3
     */
    implementation(platform("software.amazon.awssdk:bom:2.54.6"))
    implementation("software.amazon.awssdk:s3")     
    implementation("software.amazon.awssdk:url-connection-client")

    /**
     * Utils & Logging
     */
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    /**
     * Tests
     */
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-test")
    
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("com.redis:testcontainers-redis:2.2.4")

    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.awaitility:awaitility")
}

tasks.withType<Test> {
    useJUnitPlatform()

    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
    
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }  
}

tasks.bootJar {
    archiveFileName.set("service.jar")
}
