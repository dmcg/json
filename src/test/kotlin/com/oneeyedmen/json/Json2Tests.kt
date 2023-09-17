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
        val emptyList = emptyList<Any?>()
        expectThat(parse("[]")).isEqualTo(emptyList)
        expectThat(parse(" []")).isEqualTo(emptyList)
        expectThat(parse(" []  ")).isEqualTo(emptyList)
        expectThat(parse("[ ]")).isEqualTo(emptyList)
        expectThat(parse("[\n]")).isEqualTo(emptyList)
        expectThrows<IllegalArgumentException> {
            parse("[")
        }
        expectThat(parse("[\"banana\"]")).isEqualTo(listOf("banana"))
        expectThat(parse("[ null ]")).isEqualTo(listOf(null))
        expectThat(parse("[null]")).isEqualTo(listOf(null))

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
            .isEqualTo(listOf("Hello", listOf<Any?>(true, emptyList)))

        expectThat(parse("""[ {"aString"="banana"}, [true, []] ]"""))
            .isEqualTo(listOf(mapOf("aString" to "banana"), listOf(true, emptyList)))
    }

    @Test
    fun objects() {
        val emptyMap = emptyMap<String, Any?>()
        expectThat(parse("{}")).isEqualTo(emptyMap)
        expectThat(parse(" {}")).isEqualTo(emptyMap)
        expectThat(parse(" {}  ")).isEqualTo(emptyMap)
        expectThat(parse("{ }")).isEqualTo(emptyMap)
        expectThat(parse("{\n}")).isEqualTo(emptyMap)
        expectThrows<IllegalArgumentException> {
            parse("{")
        }

        expectThat(parse("""{ "aString" = "banana" }"""))
            .isEqualTo(mapOf("aString" to "banana"))
        expectThat(parse("""{"aString"="banana"}"""))
            .isEqualTo(mapOf("aString" to "banana"))
        expectThat(parse("""{"aString"=null}"""))
            .isEqualTo(mapOf("aString" to null))
        expectThat(parse("""{"aString"="{hello}"}"""))
            .isEqualTo(mapOf("aString" to "{hello}"))

        expectThat(parse("""{"aString"="{hello}", "aBoolean" = true}"""))
            .isEqualTo(mapOf("aString" to "{hello}", "aBoolean" to true))
        expectThat(parse("""{"aString"="{hello}", "anArray" = [true, false]}"""))
            .isEqualTo(mapOf("aString" to "{hello}", "anArray" to listOf(true, false)))
        expectThat(parse("""{"aString"="{hello}", "anObject" = { "aBoolean" = true}}"""))
            .isEqualTo(mapOf("aString" to "{hello}", "anObject" to mapOf("aBoolean" to true)))

    }
}


