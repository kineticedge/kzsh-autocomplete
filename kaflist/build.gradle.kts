/**
 * This is not a sub-module of the ktools project, since the setup of submodules in the parent
 * project brings in unwanted dependencies.
 */

plugins {
    java
    application
    id("org.graalvm.buildtools.native") version "0.10.2"
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("io.kineticedge.cli.KafkaList")
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:3.8.0")
    implementation("org.slf4j:slf4j-nop:2.0.16")
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.add("--verbose")
            buildArgs.add("--no-fallback")
            buildArgs.add("--allow-incomplete-classpath")
            buildArgs.add("--initialize-at-run-time=org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler")
            buildArgs.add("-H:AdditionalSecurityProviders=com.sun.security.sasl.Provider")

            val projectDir = project.projectDir.absolutePath.toString()
            val reflectConfigPath = "$projectDir/src/main/resources/META-INF/native-image/reflect-config.json"

            buildArgs.add("-H:ReflectionConfigurationFiles=$reflectConfigPath")
        }
    }
}

