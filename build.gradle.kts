import de.marcphilipp.gradle.nexus.NexusPublishPlugin
import org.jetbrains.dokka.Platform
import java.time.Duration

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("io.codearte.nexus-staging") version "0.30.0"
    id("de.marcphilipp.nexus-publish") version "0.4.0"
    id("org.jetbrains.dokka") version "1.4.32"
    signing
}
repositories {
    mavenCentral()
    jcenter()
}

val libVersion: String by project
val coroutinesVersion: String by project
val serializationVersion: String by project
val kluentVersion: String by project
val junitVersion: String by project
val sonatypeUsername: String? = System.getenv("sonatypeUsername")
val sonatypePassword: String? = System.getenv("sonatypePassword")

group = "com.apurebase"
version = libVersion

kotlin {
    explicitApi()
}

dependencies {
    implementation(kotlin("stdlib-common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks {
    test {
        useJUnitPlatform()
    }
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
    closeRepository {
        mustRunAfter(subprojects.map { it.tasks.getByName("publishToSonatype") }.toTypedArray())
    }
    closeAndReleaseRepository {
        mustRunAfter(subprojects.map { it.tasks.getByName("publishToSonatype") }.toTypedArray())
    }
    dokkaHtml {
        outputDirectory.set(buildDir.resolve("javadoc"))
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(8)
                reportUndocumented.set(true)
                platform.set(Platform.jvm)
            }
        }
    }
}


apply<NexusPublishPlugin>()

nexusPublishing {
    repositories { sonatype() }
    clientTimeout.set(Duration.parse("PT10M"))
}

nexusStaging {
    packageGroup = "com.apurebase"
    username = sonatypeUsername
    password = sonatypePassword
    numberOfRetries = 360 // 1 hour if 10 seconds delay
    delayBetweenRetriesInMillis = 10000 // 10 seconds
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    classifier = "javadoc"
    from(tasks.dokkaHtml)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            pom {
                name.set("Deferred JSON Builder")
                description.set("Deferred JSON Builder is a coroutine based pattern to split the fields and field value computation execution for generating JSON objects")
                url.set("https://github.com/aPureBase/DeferredJsonBuilder")
                organization {
                    name.set("aPureBase")
                    url.set("https://apurebase.com/")
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/aPureBase/DeferredJsonBuilder/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("jeggy")
                        name.set("Jógvan Olsen")
                        email.set("jol@apurebase.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/aPureBase/DeferredJsonBuilder.git")
                    developerConnection.set("scm:git:https://github.com/aPureBase/DeferredJsonBuilder.git")
                    url.set("https://github.com/aPureBase/DeferredJsonBuilder/")
                    tag.set("HEAD")
                }
            }
        }
    }
}

signing {
    isRequired = !version.toString().endsWith("SNAPSHOT")
    useInMemoryPgpKeys(
        System.getenv("ORG_GRADLE_PROJECT_signingKey"),
        System.getenv("ORG_GRADLE_PROJECT_signingPassword")
    )
    sign(publishing.publications["maven"])
}
