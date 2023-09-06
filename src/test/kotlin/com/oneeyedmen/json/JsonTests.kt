package com.oneeyedmen.json

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.math.BigDecimal

class JsonTests {
    @Test
    fun `test empty string`() {
        expectThrows<IllegalArgumentException> {
            parse("")
        }
        expectThrows<IllegalArgumentException> {
            parse(" ")
        }
        expectThrows<IllegalArgumentException> {
            parse("\n")
        }
    }

    @Test
    fun `null`() {
        expectThat(parse("null")).isEqualTo(null)
        expectThat(parse(" null")).isEqualTo(null)
        expectThat(parse("  null ")).isEqualTo(null)
        expectThrows<IllegalArgumentException> {
            parse("nullable")
        }
    }

    @Test
    fun booleans() {
        expectThat(parse("true")).isEqualTo(true)
        expectThat(parse(" true")).isEqualTo(true)
        expectThat(parse("  true ")).isEqualTo(true)
        expectThat(parse("false")).isEqualTo(false)
        expectThat(parse(" false")).isEqualTo(false)
        expectThat(parse("  false ")).isEqualTo(false)
    }

    @Test
    fun numbers() {
        expectThat(parse("42")).isEqualTo(BigDecimal("42"))
        expectThat(parse(" 42")).isEqualTo(BigDecimal("42"))
        expectThat(parse(" 42  ")).isEqualTo(BigDecimal("42"))
        expectThat(parse("12.34")).isEqualTo(BigDecimal("12.34"))
        expectThat(parse("12.34E5")).isEqualTo(BigDecimal("12.34E5"))
        expectThat(parse("-12.34")).isEqualTo((BigDecimal("-12.34")))

        numbers.forEach {
            expectThat(parse(it)).isA<BigDecimal>()
        }
    }

    @Test
    fun strings() {
        expectThat(parse(""""banana"""")).isEqualTo("banana")
        expectThat(parse(""" "banana"""")).isEqualTo("banana")
        expectThat(parse("""  "banana" """)).isEqualTo("banana")
        expectThat(parse(""""Hello world"""")).isEqualTo("Hello world")
        expectThat(parse("""""""")).isEqualTo("")
        expectThat(parse("""" """")).isEqualTo(" ")
        expectThat(parse(""""Hello \"world"""")).isEqualTo("Hello \"world")
        expectThrows<IllegalArgumentException> { parse(""""banana""") }

        strings.forEach {
            expectThat(parse(it)).isA<String>()
        }
    }

    @Test
    fun arrays() {
        expectThat(parse("[]")).isEqualTo(emptyList<Any?>())
        expectThat(parse(" []")).isEqualTo(emptyList<Any?>())
        expectThat(parse(" []  ")).isEqualTo(emptyList<Any?>())
        expectThat(parse("[ ]")).isEqualTo(emptyList<Any?>())
        expectThat(parse("[\n]")).isEqualTo(emptyList<Any?>())
        expectThrows<IllegalArgumentException> { parse("[") }

        expectThat(parse("[\"banana\"]")).isEqualTo(listOf("banana"))
        expectThat(parse("[ null ]")).isEqualTo(listOf(null))

        expectThat(parse("[ \"banana\", null ]")).isEqualTo(listOf("banana", null))
//        expectThat(parse("[ \"Hello, World\", null ]")).isEqualTo(listOf("Hello, World", null))
    }
}

fun parse(json: String): Any? {
    val trimmed = json.trim()
    return when {
        trimmed == "null" -> null
        trimmed == "true" -> true
        trimmed == "false" -> false
        trimmed.isString() -> parseString(trimmed)
        trimmed.isArray() -> parseArray(trimmed)
        else -> trimmed.toBigDecimalOrNull() ?:
            throw IllegalArgumentException("Not valid json <$json>")
    }
}

private fun parseString(trimmed: String) = trimmed.subSequence(1, trimmed.length - 1).withEscapesExpanded()

private fun parseArray(trimmed: String): List<Any?> {
    val content = trimmed.subSequence(1, trimmed.length - 1)
    return if (content.isBlank()) emptyList<Any?>()
    else content.toString().split(',').map {
        parse(it)
    }
}

private fun String.isArray() = startsWith('[') && endsWith(']')

private fun String.isString() = startsWith('\"') && endsWith('\"')

private fun CharSequence.withEscapesExpanded(): String =
    this.toString().replace("""\"""", "\"")
