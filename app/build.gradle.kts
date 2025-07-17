plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.mi.ksp)
}

android {
    namespace = "com.grupoalv.procesadorgenerateproto"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.grupoalv.procesadorgenerateproto"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    generateProtoTasks {
        tasks.named("kspDebugKotlin") {
            finalizedBy("copyGeneratedProtoFiles")
        }
        all().forEach {
            it.builtins {
                register("java") {
                    option("lite") // Necesario para Android
                }
            }
        }
    }
}
tasks.register("copyGeneratedProtoFiles") {

    doLast {
        val kspOutputDir = file("build/generated/ksp/debug/resources")
        val protoDir = file("src/main/proto")

        if (kspOutputDir.exists()) {
            // Crear el directorio proto si no existe
            protoDir.mkdirs()

            // Copiar todos los archivos .proto generados
            kspOutputDir.walkTopDown()
                .filter { it.extension == "proto" }
                .forEach { protoFile ->
                    val targetFile = File(protoDir, protoFile.name)
                    protoFile.copyTo(targetFile, overwrite = true)
                    println("📄 Copiado: ${protoFile.name} a src/main/proto/")
                }
        } else {
            println("⚠️ No se encontró el directorio KSP: $kspOutputDir")
        }
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    implementation(libs.generateproto)
    ksp(project(":Procesador"))
    implementation(project(":Procesador"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}