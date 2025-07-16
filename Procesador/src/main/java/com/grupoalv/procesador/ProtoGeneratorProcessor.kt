package com.grupoalv.procesador

import com.grupoalv.generateproto.GenerateProto
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.util.Locale

class ProtoGeneratorProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val processedClasses = mutableSetOf<String>()
    private val generatedEnums = mutableSetOf<String>()
    private val requiredImports = mutableSetOf<String>()
    private val generatedFiles = mutableSetOf<String>() // Para evitar sobrescribir archivos
    // --- NUEVO: Mapa global para nombres proto únicos ---
    private val protoNameMap = mutableMapOf<Pair<String, String>, String>()
    private val classNameCount = mutableMapOf<String, Int>()
    private val classNameToPackages = mutableMapOf<String, MutableList<String>>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("🚀 ProtoGeneratorProcessor.process() INICIADO")
        
        try {
            val annotationName = GenerateProto::class.qualifiedName ?: return emptyList()
            logger.warn("🔍 Buscando símbolos con anotación: $annotationName")

            val symbols = resolver.getSymbolsWithAnnotation(annotationName)
            val symbolsList = symbols.toList()
            logger.warn("📊 Encontrados ${symbolsList.size} símbolos con la anotación")

            symbolsList.forEach { symbol ->
                logger.warn("📋 Símbolo encontrado: ${symbol::class.simpleName} - ${symbol}")
            }

            val classDeclarations = symbolsList.filterIsInstance<KSClassDeclaration>()
            logger.warn("🏗️ Clases encontradas: ${classDeclarations.size}")

            // Procesar enums primero
            classDeclarations.filter { it.classKind == com.google.devtools.ksp.symbol.ClassKind.ENUM_CLASS }
                .forEach { enumDecl ->
                    logger.warn("🎯 Procesando enum: ${enumDecl.simpleName.asString()}")
                    generateEnumProto(enumDecl)
                }

            // Luego procesar data classes
            classDeclarations.filter { it.classKind == com.google.devtools.ksp.symbol.ClassKind.CLASS }
                .forEach { classDecl ->
                    logger.warn("🎯 Procesando clase: ${classDecl.simpleName.asString()}")
                    generateProto(classDecl, resolver)
                }

            logger.warn("✅ ProtoGeneratorProcessor.process() COMPLETADO")
            return emptyList()
        } catch (e: Exception) {
            logger.error("❌ Error en ProtoGeneratorProcessor: ${e.message}\n${e.stackTraceToString()}")
            throw e
        }
    }

    private fun generateEnumProto(enumDecl: KSClassDeclaration) {
        val enumName = enumDecl.simpleName.asString()
        
        val pkgName = getPackageName(enumDecl)
        val fullName = "$pkgName.$enumName"
        
        if (generatedEnums.contains(fullName)) return
        
        logger.warn("🔧 Generando enum proto para: $enumName en $pkgName")
        logger.warn("📦 Package name: $pkgName")
        
        // Para enums, necesitamos obtener las entradas de manera diferente
        val enumEntries = enumDecl.declarations.filter { declaration ->
            val name = declaration.simpleName.asString()
            name != "values" && 
            name != "valueOf" &&
            name != "entries" &&
            name != "<init>" &&
            name != "<clinit>" &&
            !name.startsWith("get") &&
            !name.startsWith("set") &&
            declaration is com.google.devtools.ksp.symbol.KSClassDeclaration
        }.toList()
        logger.warn("📝 Entradas de enum encontradas: ${enumEntries.size}")
        
        // --- USAR NOMBRE ÚNICO ---
        val protoName = getOrCreateProtoName(pkgName, enumName)
        val lines = mutableListOf(
            "syntax = \"proto3\";",
            "",
            "option java_package = \"$pkgName\";",
            "",
            "enum $protoName {"
        )
        
        enumEntries.forEachIndexed { index, entry ->
            val entryName = entry.simpleName.asString()
            val enumLine = "  ${entryName.uppercase()} = $index;"
            lines.add(enumLine)
            logger.warn("🔤 Entrada de enum agregada: $enumLine")
        }
        
        lines.add("}")
        
        try {
            val protoContent = lines.joinToString("\n")
            
            // Generar nombre único para el archivo
            val fileName = protoName
            
            val kspFile = codeGenerator.createNewFile(
                Dependencies(false),
                pkgName,
                fileName,
                "proto"
            )
            kspFile.bufferedWriter().use { it.write(protoContent) }
            logger.warn("📄 Archivo enum proto generado: $fileName.proto en $pkgName")
            generatedEnums.add(fullName)
            
        } catch (e: Exception) {
            logger.error("❌ Error al crear archivo enum proto para $enumName: ${e.message}")
        }
    }

    private fun generateProto(classDecl: KSClassDeclaration, resolver: Resolver) {
        val className = classDecl.simpleName.asString()
        val pkgName = getPackageName(classDecl)
        val fullName = "$pkgName.$className"
        
        if (processedClasses.contains(fullName)) return
        
        logger.warn("🔧 Generando proto para: $className en $pkgName")
        logger.warn("📦 Package name: $pkgName")

        // Limpiar imports para esta clase específica
        requiredImports.clear()
        
        val fields = classDecl.getAllProperties().toList()
        logger.warn("📝 Propiedades encontradas: ${fields.size}")
        
        // Procesar campos para detectar imports
        fields.forEach { prop ->
            resolveFieldType(prop, resolver)
        }
        
        // --- USAR NOMBRE ÚNICO ---
        val protoName = getOrCreateProtoName(pkgName, className)
        val lines = mutableListOf(
            "syntax = \"proto3\";",
            ""
        )
        
        // Agregar imports necesarios
        if (requiredImports.isNotEmpty()) {
            requiredImports.forEach { importName ->
                lines.add("import \"$importName.proto\";")
            }
            lines.add("")
        }
        
        lines.add("option java_package = \"$pkgName\";")
        lines.add("")
        lines.add("message $protoName {")

        fields.forEachIndexed { index, prop ->
            val fieldType = resolveFieldType(prop, resolver, forImport = false)
            val fieldName = prop.simpleName.asString()
            val fieldLine = "  $fieldType $fieldName = ${index + 1};"
            lines.add(fieldLine)
            logger.warn("🔤 Campo agregado: $fieldLine")
        }

        lines.add("}")

        try {
            val protoContent = lines.joinToString("\n")
            
            // Generar nombre único para el archivo
            val fileName = protoName
            
            val kspFile = codeGenerator.createNewFile(
                Dependencies(false),
                pkgName,
                fileName,
                "proto"
            )
            kspFile.bufferedWriter().use { it.write(protoContent) }
            logger.warn("📄 Archivo proto generado: $fileName.proto en $pkgName")
            processedClasses.add(fullName)
            
        } catch (e: Exception) {
            logger.error("❌ Error al crear archivo proto para $className: ${e.message}")
        }
    }

    private fun getPackageName(classDecl: KSClassDeclaration): String {
        val generateProtoAnnotation = classDecl.annotations.firstOrNull {
            it.shortName.asString() == "GenerateProto"
        }
        requireNotNull(generateProtoAnnotation) { "La clase ${classDecl.simpleName.asString()} debe tener la anotación @GenerateProto" }
        val pkgName = generateProtoAnnotation.arguments.firstOrNull()?.value as? String
        require(!pkgName.isNullOrBlank()) { "El atributo packageName de @GenerateProto no puede ser nulo o vacío en ${classDecl.simpleName.asString()}" }
        return pkgName
    }

    // --- NUEVO: Generador de nombre proto único y consistente SOLO con sufijo si hay colisión, usando CamelCase ---
    private fun getOrCreateProtoName(pkgName: String, className: String): String {
        val key = pkgName to className
        protoNameMap[key]?.let { return it }
        // Registrar el paquete para este nombre de clase
        val pkgs = classNameToPackages.getOrPut(className) { mutableListOf() }
        if (!pkgs.contains(pkgName)) pkgs.add(pkgName)
        val count = pkgs.size
        val base = "Proto" + className.replaceFirstChar { it.uppercase() }
        val unique = if (count == 1) {
            // Primer uso: sin sufijo
            base
        } else {
            // Colisión: agregar sufijo del último segmento del paquete en CamelCase
            val pkgSegments = pkgName.split('.')
            val lastSegment = pkgSegments.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "Other"
            base + lastSegment
        }
        protoNameMap[key] = unique
        return unique
    }

    // --- MODIFICAR: resolveFieldType para usar el nuevo nombre proto en imports y tipos ---
    private fun resolveFieldType(prop: KSPropertyDeclaration, resolver: Resolver, forImport: Boolean = true): String {
        val type = prop.type.resolve()
        val typeName = type.declaration.simpleName.asString()
        val classDecl = type.declaration as? KSClassDeclaration

        // Mapear tipos básicos
        val kotlinToProto = mapOf(
            "Int" to "int32",
            "Long" to "int64",
            "String" to "string",
            "Boolean" to "bool",
            "Float" to "float",
            "Double" to "double"
        )
        if (kotlinToProto.containsKey(typeName)) {
            return kotlinToProto[typeName]!!
        }
        val isList = type.declaration.qualifiedName?.asString() == "kotlin.collections.List"
        val isMap = type.declaration.qualifiedName?.asString() == "kotlin.collections.Map"
        val isNullable = prop.type.resolve().nullability.name == "NULLABLE"
        val isEnum = classDecl?.classKind == com.google.devtools.ksp.symbol.ClassKind.ENUM_CLASS
        val isDataClass = classDecl?.classKind == com.google.devtools.ksp.symbol.ClassKind.CLASS

        if (isList) {
            val argType = type.arguments.firstOrNull()?.type?.resolve()
            val argTypeName = argType?.declaration?.simpleName?.asString() ?: "Any"
            val argClassDecl = argType?.declaration as? KSClassDeclaration
            val repeatedType = if (kotlinToProto.containsKey(argTypeName)) {
                kotlinToProto[argTypeName]!!
            } else if (argClassDecl != null && argClassDecl.annotations.any { it.shortName.asString() == "GenerateProto" }) {
                val argPkg = getPackageName(argClassDecl)
                val repeatedProtoName = getOrCreateProtoName(argPkg, argTypeName)
                if (forImport && argPkg.isNotEmpty()) requiredImports.add(repeatedProtoName)
                repeatedProtoName
            } else {
                "string"
            }
            return "repeated $repeatedType"
        }
        if (isMap) {
            return "map<string, string>"
        }
        if (isEnum || isDataClass) {
            if (classDecl != null && classDecl.annotations.any { it.shortName.asString() == "GenerateProto" }) {
                val pkgName = getPackageName(classDecl)
                val protoName = getOrCreateProtoName(pkgName, typeName)
                if (forImport && pkgName.isNotEmpty()) requiredImports.add(protoName)
                return protoName
            } else {
                return "string"
            }
        }
        if (isNullable) {
            return resolveFieldType(prop, resolver, forImport)
        }
        return "string"
    }

    private val basicTypes = setOf("Int", "Long", "String", "Boolean", "Float", "Double", "Byte", "Short", "Char")

    private fun generateUniqueFileName(packageName: String, className: String): String {
        // Crear un nombre único basado en el package y la clase
        val packageSuffix = packageName.split(".").lastOrNull() ?: ""
        return if (packageSuffix.isNotEmpty() && packageSuffix != "pruebasproto") {
            "${className}_${packageSuffix.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.ROOT
                ) else it.toString()
            }}"
        } else {
            className
        }
    }

    private fun mapKotlinToProto(type: String) = when (type) {
        "Int" -> "int32"
        "Long" -> "int64"
        "String" -> "string"
        "Boolean" -> "bool"
        "Float" -> "float"
        "Double" -> "double"
        "Byte" -> "int32"
        "Short" -> "int32"
        "Char" -> "string"
        else -> "string" // fallback
    }
}
