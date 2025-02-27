plugins {
    id("com.diffplug.spotless") version "latest.release"
}

repositories {
    mavenCentral()
}


spotless {
    java {
        target("**/src/**/java/**/*.java")
        removeUnusedImports()
        importOrder()
        clangFormat("19.1.7").style("file:config/clang-format")
        licenseHeader("/* © SRSoftware 2024 */")
    }
}


subprojects {
    group = "de.srsoftware"
    version = "1.0-SNAPSHOT"

    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenLocal()
        mavenCentral()
    }



    val implementation by configurations
    val compileOnly by configurations
    val testImplementation by configurations
    val testRuntimeOnly by configurations


    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.withType<Test>() {
        useJUnitPlatform()
    }
}