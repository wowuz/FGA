package io.github.lib_automata

interface OcrService {
    suspend fun detectText(pattern: Pattern): String
}