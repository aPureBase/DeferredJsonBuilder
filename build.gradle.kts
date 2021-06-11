plugins {
    kotlin("jvm") version "1.5.10"
}
repositories {
    mavenCentral()
    jcenter()
}

group = "com.apurebase"
version = "0.0.1"


// Version Numbers
val coroutinesVersion = "1.5.0"
val serializationVersion = "1.2.1"
val kluentVersion = "1.60"
val junitVersion = "5.7.2"


dependencies {
    implementation(kotlin("stdlib-common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}


tasks.withType<Test> {
    useJUnitPlatform()
}

