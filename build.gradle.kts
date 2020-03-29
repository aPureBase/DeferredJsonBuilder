plugins {
    kotlin("jvm") version "1.3.71"
}
repositories {
    jcenter()
}

group = "com.apurebase"
version = "0.0.1"


// Version Numbers
val coroutinesVersion = "1.3.5"
val serializationVersion = "0.20.0"
val kluentVersion = "1.60"
val junitVersion = "5.6.1"


dependencies {
    implementation(kotlin("stdlib-common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")

    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}


tasks.withType<Test> {
    useJUnitPlatform()
}

