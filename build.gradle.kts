import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.gang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.codemc.org/repository/maven-public/")
    maven ( "https://jitpack.io")
    maven(url = "https://repo.codemc.org/repository/maven-public/")

}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    shadow("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("dev.jorel:commandapi-bukkit-shade:9.5.2")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.19.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.19.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC.2")
    compileOnly("io.github.monun:invfx-api:3.3.2")
    compileOnly ("com.github.MilkBowl:VaultAPI:1.7")
    implementation("dev.jorel:commandapi-bukkit-kotlin:9.5.2")

}

val targetJavaVersion = 17
kotlin {
    jvmToolchain(targetJavaVersion)
}
tasks.register<Copy>("copyPlugin") {
    from(tasks.named("jar"))
    into(file("C:\\Users\\wagwa\\Downloads\\paper 1.20.4\\plugins"))
}

tasks.named("build") {
    finalizedBy("copyPlugin")
}
tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
tasks.withType<ShadowJar> {
    dependencies {
        include(dependency("dev.jorel:commandapi-bukkit-shade:9.5.2"))
    }
    relocate("dev.jorel.commandapi", "org.gang.customShop.commandapi")
}