plugins {
    java
}

group = "me.leeseol"
version = "0.1.0"

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("LeeSeolCore")
}
