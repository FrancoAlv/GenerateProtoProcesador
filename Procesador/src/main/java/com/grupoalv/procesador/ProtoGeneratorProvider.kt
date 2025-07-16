package com.grupoalv.procesador

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ProtoGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.warn("🏭 ProtoGeneratorProvider.create() INVOCADO - KSP encontró el provider!")
        val processor = ProtoGeneratorProcessor(environment.codeGenerator, environment.logger)
        environment.logger.warn("🏭 ProtoGeneratorProcessor creado exitosamente")
        return processor
    }
}
