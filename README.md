# Procesador GenerateProto

![KSP](https://img.shields.io/badge/Kotlin%20Symbol%20Processing-KSP-blue)

## ¿Qué es este procesador?

Este proyecto es un **procesador de anotaciones KSP** (Kotlin Symbol Processing) que genera automáticamente archivos `.proto` a partir de clases y enums de Kotlin anotados con `@GenerateProto`. Su objetivo es facilitar la interoperabilidad y la generación de contratos Protobuf de manera automática y consistente, evitando errores manuales y acelerando el desarrollo de APIs y servicios.

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

## Créditos
Desarrollado por [Tu Nombre o Equipo].

---

¿Dudas o sugerencias? ¡Abre un issue o contacta al equipo! 