# Procesador GenerateProto

![KSP](https://img.shields.io/badge/Kotlin%20Symbol%20Processing-KSP-blue)
![Protobuf](https://img.shields.io/badge/Protocol%20Buffers-Protobuf-green)

## ¿Qué es este procesador?

Este proyecto es un **procesador de anotaciones KSP** (Kotlin Symbol Processing) que genera automáticamente archivos `.proto` a partir de clases y enums de Kotlin anotados con `@GenerateProto`. Su objetivo es facilitar la interoperabilidad y la generación de contratos Protobuf de manera automática y consistente, evitando errores manuales y acelerando el desarrollo de APIs y servicios.

---

## ⚠️ Requisitos obligatorios para usar el procesador

Para que el procesador funcione correctamente, **debes** tener en tu `build.gradle.kts` (o `build.gradle`) lo siguiente:

### 1. Plugins necesarios

**Estos plugins son OBLIGATORIOS para que el procesador funcione:**

```kotlin
plugins {
    alias(libs.plugins.protobuf)      // Plugin de Protobuf
    alias(libs.plugins.mi.ksp)        // Plugin de KSP
    // ...otros plugins necesarios para tu proyecto
}
```

**Versiones recomendadas en `libs.versions.toml`:**
```toml
[versions]
protobufPlugin = "0.9.5"
kotlin = "2.0.21"
symbolProcessingApi = "2.0.21-1.0.25"

[plugins]
mi-ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.25" }
protobuf = { id = "com.google.protobuf", version.ref = "protobufPlugin" }
```

### 2. Configuración de Protobuf

```kotlin
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
```

### 3. Tareas de limpieza y copia de archivos `.proto`

```kotlin
// Limpia los archivos generados por KSP
tasks.register("cleanKspGenerated", Delete::class) {
    delete("build/generated/ksp/")
}

// Limpia los archivos .proto generados/copied en src/main/proto
tasks.register("cleanProtoGenerated", Delete::class) {
    delete("src/main/proto/")
}

// Task para copiar archivos proto generados por KSP a src/main/proto
tasks.register("copyGeneratedProtoFiles") {
    doLast {
        val kspOutputDir = file("build/generated/ksp/debug/resources")
        val protoDir = file("src/main/proto")
        if (kspOutputDir.exists()) {
            protoDir.mkdirs()
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
```

### 4. Dependencias requeridas

```kotlin
dependencies {
    ksp("com.github.FrancoAlv:GenerateProtoProcesador:5.0.0")
    implementation("com.github.FrancoAlv:GenerateProtoProcesador:5.0.0")
    implementation("com.github.FrancoAlv:GenerateProto:2.0.0")
    // ...otras dependencias necesarias
}
```

> **IMPORTANTE:**
> - Sin estas configuraciones, el procesador **no generará ni copiará** los archivos `.proto` correctamente.
> - Asegúrate de tener los plugins y dependencias en tu archivo `libs.versions.toml` si usas versiones centralizadas.

---

## ¿Cómo funciona?

1. **Detecta** todas las clases y enums anotados con `@GenerateProto` en tu código fuente.
2. **Genera** archivos `.proto` para cada clase y enum, respetando el paquete y nombre especificados en la anotación.
3. **Resuelve dependencias** entre clases, generando los imports necesarios en los archivos `.proto`.
4. **Evita colisiones** de nombres generando nombres únicos y consistentes para los mensajes y enums.
5. **Soporta**:
   - Tipos primitivos (`Int`, `String`, `Boolean`, etc.)
   - Listas (`List<T>`) y mapas simples
   - Clases anidadas y enums personalizados

---

## Ejemplo de uso

Supón que tienes la siguiente clase:

```kotlin
@GenerateProto(packageName = "com.ejemplo.dominio")
data class Usuario(
    val id: Int,
    val nombre: String,
    val roles: List<Rol>
)

@GenerateProto(packageName = "com.ejemplo.dominio")
enum class Rol {
    ADMIN, USER, GUEST
}
```

El procesador generará automáticamente los archivos `ProtoUsuario.proto` y `ProtoRol.proto` en el paquete correspondiente, con la estructura adecuada para Protobuf.

---

## ¿Cómo lo uso en mi proyecto?

1. **Agrega el procesador como dependencia** en tu módulo KSP:
   ```kotlin
   dependencies {
       ksp(project(":Procesador"))
   }
   ```
2. **Anota tus clases y enums** con `@GenerateProto`, especificando el `packageName` destino.
3. **Compila tu proyecto**. Los archivos `.proto` se generarán automáticamente en la carpeta de salida de KSP.

---

## Ventajas
- **Automatización**: Olvídate de escribir archivos `.proto` manualmente.
- **Consistencia**: Nombres únicos y sin colisiones.
- **Escalabilidad**: Soporta proyectos grandes y estructuras complejas.
- **Fácil integración**: Solo necesitas anotar tus clases.

---

## Estructura del proyecto

```
ProcesadorGenerateProto/
├── app/                    # Aplicación de ejemplo
├── Procesador/            # Módulo del procesador KSP
│   ├── src/main/java/
│   │   └── com/grupoalv/procesador/
│   │       ├── ProtoGeneratorProcessor.kt
│   │       └── ProtoGeneratorProvider.kt
│   └── README.md          # Documentación detallada
└── README.md              # Este archivo
```

---

## Créditos
Desarrollado por [Tu Nombre o Equipo].

---

¿Dudas o sugerencias? ¡Abre un issue o contacta al equipo! 