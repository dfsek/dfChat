plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}


repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

dependencies {
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("com.android.tools.build:gradle:7.3.0")
    api(kotlin("gradle-plugin:1.7.20"))
}

gradlePlugin {
    plugins {
        register("composeFixer") {
            id = "com.dfsek.dfchat.compose-fixer"
            implementationClass = "TransformPlugin"
        }
    }
}