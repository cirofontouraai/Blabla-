package com.example.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation that formats raw 11 digits phone numbers into
 * the standard Brazilian format: (XX) XXXXX-XXXX in real time.
 */
class PhoneMaskVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val trimmed = if (raw.length > 11) raw.substring(0, 11) else raw
        
        val out = StringBuilder()
        for (i in trimmed.indices) {
            when (i) {
                0 -> out.append("(").append(trimmed[i])
                1 -> out.append(trimmed[i]).append(") ")
                6 -> out.append("-").append(trimmed[i])
                else -> out.append(trimmed[i])
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val length = trimmed.length
                val actualOffset = if (offset > length) length else offset
                
                return when {
                    actualOffset <= 1 -> actualOffset + 1  // Inside the DDD (e.g. "(1")
                    actualOffset <= 6 -> actualOffset + 3  // After ")" and space (e.g. "(11) 9876")
                    actualOffset <= 11 -> actualOffset + 4 // After dash (e.g. "(11) 98765-4321")
                    else -> 15
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                return when {
                    offset <= 2 -> (offset - 1).coerceAtLeast(0)
                    offset <= 9 -> (offset - 3).coerceAtLeast(0)
                    offset <= 15 -> (offset - 4).coerceAtLeast(0)
                    else -> 11
                }
            }
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}
