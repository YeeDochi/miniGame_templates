plugins {
    java
    id("org.springframework.boot") version "3.2.1" // 최신 안정 버전 사용 권장
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // JDK 17 기준
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 웹소켓 필수
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // 뷰 템플릿 (필요시 사용, 없어도 무방)
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // 롬복 (Lombok)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
