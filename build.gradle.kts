import net.fabricmc.loom.RunConfig
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    alias(libs.plugins.voldeloom)
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

val modVersion: String by project
version = modVersion

val modGroup: String by project
group= modGroup

val modBaseName: String by project

val jdkVersion = 11
val compileTargetVersion = 6

dependencies {
    minecraft(libs.minecraft)
    forge(variantOf(libs.forge) { classifier("universal"); artifactType("zip") })
    mappings(variantOf(libs.forge) { classifier("src"); artifactType("zip") })

    compileOnly(libs.jsr305)

    modImplementation(files("libs/BetterQuesting-3.0.328-dev.jar"))

    // FIXME add deps
    //compile "codechicken:NotEnoughItems:1.7.10-1.0.5.120:dev"
    //compile "codechicken:CodeChickenLib:1.7.10-1.1.3.140:dev"
    //compile "codechicken:CodeChickenCore:1.7.10-1.0.7.47:dev"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }
}

tasks {
    compileJava {
        options.release.set(compileTargetVersion)
        // Show all compile errors
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
    }
    compileTestJava {
        options.release.set(jdkVersion)
    }
    processResources {
        filesMatching("mcmod.info") {
            expand("version" to project.version, "mcversion" to libs.versions.minecraft.get())
        }
    }
    test {
        useJUnitPlatform()
    }
    jar {
        archiveBaseName.set(modBaseName)
        archiveClassifier.set("core")
    }
}

volde {
    runs {
        getByName<RunConfig>("client") {
            programArg("Player")
        }
    }
}

