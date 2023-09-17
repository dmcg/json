package com.oneeyedmen.json

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.math.BigDecimal

class Json2Tests {
    @Test
    fun `empty string`() {
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
    fun strings() {
        expectThat(parse("""""""")).isEqualTo("")
        expectThat(parse(""""banana"""")).isEqualTo("banana")
        expectThat(parse(""" "banana"""")).isEqualTo("banana")
        expectThat(parse("""  "banana" """)).isEqualTo("banana")
        expectThat(parse(""""Hello world"""")).isEqualTo("Hello world")
        expectThat(parse("""" """")).isEqualTo(" ")
        expectThat(parse(""""Hello \"world"""")).isEqualTo("Hello \"world")
        expectThrows<IllegalArgumentException> {
            parse(""""banana""")
        }

        strings.forEach {
            expectThat(parse(it)).isA<String>()
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
        // TODO
//        expectThrows<IllegalArgumentException> {
//            parse("null,")
//        }
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
    fun arrays() {
        expectThat(parse("[]")).isEqualTo(emptyList<Any?>())
        expectThat(parse(" []")).isEqualTo(emptyList<Any?>())
        expectThat(parse(" []  ")).isEqualTo(emptyList<Any?>())
        expectThat(parse("[ ]")).isEqualTo(emptyList<Any?>())
        expectThat(parse("[\n]")).isEqualTo(emptyList<Any?>())
        expectThrows<IllegalArgumentException> {
            parse("[")
        }
        expectThat(parse("[\"banana\"]")).isEqualTo(listOf("banana"))
        expectThat(parse("[ null ]")).isEqualTo(listOf(null))

        expectThat(parse("[ \"banana\", null ]"))
            .isEqualTo(listOf("banana", null))
        expectThat(parse("[ \"banana\" null ]"))
            .isEqualTo(listOf("banana", null))
        expectThat(parse("[ true, false ]"))
            .isEqualTo(listOf(true, false))
        // TODO show lists need comma separators
        expectThat(parse("[ \"Hello, World\", null ]"))
            .isEqualTo(listOf("Hello, World", null))
        expectThat(parse("[ \"[Hello], World\", null ]"))
            .isEqualTo(listOf("[Hello], World", null))

        expectThat(parse("[ \"Hello\", [true, []] ]"))
            .isEqualTo(listOf("Hello", listOf<Any?>(true, emptyList<Any?>())))

    }
}


