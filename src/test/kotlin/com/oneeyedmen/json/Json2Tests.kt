package com.oneeyedmen.json

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.message
import java.math.BigDecimal

class Json2Tests {
    @Test
    fun `empty or blank string`() {
        expectThrows<IllegalArgumentException> {
            parse("")
        }.message.isEqualTo("No top-level content found")
        expectThrows<IllegalArgumentException> {
            parse(" ")
        }.message.isEqualTo("No top-level content found")
        expectThrows<IllegalArgumentException> {
            parse("\n")
        }.message.isEqualTo("No top-level content found")
    }

    @Test
    fun strings() {
        expectThat(parse("""""""")).isEqualTo("")
        expectThat(parse(""""banana"""")).isEqualTo("banana")
        expectThat(parse(""" "banana"""")).isEqualTo("banana")
        expectThat(parse("""  "banana" """)).isEqualTo("banana")
        expectThat(parse(""""Hello world"""")).isEqualTo("Hello world")
        expectThat(parse("""" """")).isEqualTo(" ")
        expectThrows<IllegalArgumentException> {
            parse(""""banana""")
        }

        strings.forEach {
            expectThat(parse(it)).isA<String>()
        }
    }

    @Test
    fun `string escapes`() {
        expectThat(parse(""""Hello \"world"""")).isEqualTo("Hello \"world")
        expectThat(parse(""""Hello \\world"""")).isEqualTo("Hello \\world")
        expectThat(parse(""""Hello \/world"""")).isEqualTo("Hello /world")
        expectThat(parse(""""Hello \bworld"""")).isEqualTo("Hello \bworld")
        expectThat(parse(""""Hello \nworld"""")).isEqualTo("Hello \nworld")
        expectThat(parse(""""Hello \rworld"""")).isEqualTo("Hello \rworld")
        expectThat(parse(""""Hello \tworld"""")).isEqualTo("Hello \tworld")
        expectThat(parse(""""Hello \fworld"""")).isEqualTo("Hello \u000Cworld")
        expectThrows<IllegalArgumentException> {
            parse(""""Hello \xworld"""")
        }.message.isEqualTo("Illegal escape <\\x>")

        expectThat(parse(""""Hello \u000Cworld"""")).isEqualTo("Hello \u000Cworld")
        expectThat(parse(""""Hello \u9999world"""")).isEqualTo("Hello \u9999world")
        expectThrows<IllegalArgumentException> {
            parse(""""Hello \ux world"""")
        }.message.isEqualTo("Illegal unicode escape <\\ux>")
        expectThrows<IllegalArgumentException> {
            parse(""""Hello \u9x world"""")
        }.message.isEqualTo("Illegal unicode escape <\\u9x>")
        expectThrows<IllegalArgumentException> {
            parse(""""Hello \u987 world"""")
        }.message.isEqualTo("Illegal unicode escape <\\u987>")
        expectThrows<IllegalArgumentException> {
            parse(""""Hello \u987"""")
        }.message.isEqualTo("Illegal unicode escape <\\u987>")
    }

    @Test
    fun `null`() {
        expectThat(parse("null")).isEqualTo(null)
        expectThat(parse(" null")).isEqualTo(null)
        expectThat(parse("  null ")).isEqualTo(null)
        expectThrows<IllegalArgumentException> {
            parse("nullable")
        }.message.isEqualTo("Illegal literal character <b>")
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
    fun `doesnt allow multiple top level things`() {
        expectThrows<IllegalArgumentException> {
            parse("true false")
        }.and {
            message.isEqualTo("Cannot have more than one top level result, failed at <f>")
        }
        expectThrows<IllegalArgumentException> {
            parse("null,")
        }.and {
            message.isEqualTo("Not a valid top-level character <,>")
        }
        expectThrows<IllegalArgumentException> {
            parse("null ,")
        }.and {
            message.isEqualTo("Not a valid top-level character <,>")
        }
        expectThrows<IllegalArgumentException> {
            parse("null:")
        }.and {
            message.isEqualTo("Not a valid top-level character <:>")
        }
        expectThrows<IllegalArgumentException> {
            parse("\"banana\",")
        }.and {
            message.isEqualTo("Not a valid top-level character <,>")
        }
        expectThrows<IllegalArgumentException> {
            parse("[],")
        }.and {
            message.isEqualTo("Not a valid top-level character <,>")
        }
        expectThrows<IllegalArgumentException> {
            parse("{},")
        }.and {
            message.isEqualTo("Not a valid top-level character <,>")
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
        expectThat(parse("[\"banana\"]")).isEqualTo(listOf("banana"))
        expectThat(parse("[ null ]")).isEqualTo(listOf(null))
        expectThat(parse("[null]")).isEqualTo(listOf(null))

        expectThat(parse("[ \"banana\", null ]"))
            .isEqualTo(listOf("banana", null))
        expectThat(parse("[ true, false ]"))
            .isEqualTo(listOf(true, false))
        expectThat(parse("[ \"Hello, World\", null ]"))
            .isEqualTo(listOf("Hello, World", null))
        expectThat(parse("[ \"[Hello], World\", null ]"))
            .isEqualTo(listOf("[Hello], World", null))

        expectThat(parse("[ \"Hello\", [true, []] ]"))
            .isEqualTo(listOf("Hello", listOf<Any?>(true, emptyList)))

        expectThat(parse("""[ {"aString":"banana"}, [true, []] ]"""))
            .isEqualTo(listOf(mapOf("aString" to "banana"), listOf(true, emptyList)))

    }

    @Test
    fun `unterminated arrays`() {
        expectThrows<IllegalArgumentException> {
            parse("[")
        }.message.isEqualTo("Unterminated array")
        expectThrows<IllegalArgumentException> {
            parse("[ true")
        }.message.isEqualTo("Unterminated array")
        expectThrows<IllegalArgumentException> {
            parse("[ true,")
        }.message.isEqualTo("Unterminated array")
    }

    @Test
    fun `arrays need comma separators`() {
        expectThrows<IllegalArgumentException> {
            parse("[true false \"banana\"]")
        }.message.isEqualTo("Expected a comma in an array, got <f>")
        expectThrows<IllegalArgumentException> {
            parse("[true, false \"banana\"]")
        }.message.isEqualTo("Expected a comma in an array, got <\">")
        expectThrows<IllegalArgumentException> {
            parse("[true,]")
        }.message.isEqualTo("Unexpected character in array <]>")
        expectThrows<IllegalArgumentException> {
            parse("[true ,]")
        }.message.isEqualTo("Unexpected character in array <]>")
        expectThrows<IllegalArgumentException> {
            parse("[true , ]")
        }.message.isEqualTo("Unexpected character in array <]>")
        expectThrows<IllegalArgumentException> {
            parse("[,]")
        }.message.isEqualTo("Unexpected character in array <,>")
        expectThrows<IllegalArgumentException> {
            parse("[ ,]")
        }.message.isEqualTo("Unexpected character in array <,>")
        expectThrows<IllegalArgumentException> {
            parse("[ , ]")
        }.message.isEqualTo("Unexpected character in array <,>")
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

        expectThat(parse("""{ "aString" : "banana" }"""))
            .isEqualTo(mapOf("aString" to "banana"))
        expectThat(parse("""{"aString":"banana"}"""))
            .isEqualTo(mapOf("aString" to "banana"))
        expectThat(parse("""{"aString":null}"""))
            .isEqualTo(mapOf("aString" to null))
        expectThat(parse("""{"aString":"{hello}"}"""))
            .isEqualTo(mapOf("aString" to "{hello}"))

        expectThat(parse("""{"aString":"{hello}", "aBoolean" : true}"""))
            .isEqualTo(mapOf("aString" to "{hello}", "aBoolean" to true))
        expectThat(parse("""{"aString":null, "aBoolean" : true}"""))
            .isEqualTo(mapOf("aString" to null, "aBoolean" to true))
        expectThat(parse("""{"aString":"{hello}", "anArray" : [true, false]}"""))
            .isEqualTo(mapOf("aString" to "{hello}", "anArray" to listOf(true, false)))
        expectThat(parse("""{"aString":"{hello}", "anObject" : { "aBoolean" : true}}"""))
            .isEqualTo(mapOf("aString" to "{hello}", "anObject" to mapOf("aBoolean" to true)))
    }

    @Test
    fun `unterminated objects`() {
        expectThrows<IllegalArgumentException> {
            parse("{")
        }.message.isEqualTo("Unterminated object")
        expectThrows<IllegalArgumentException> {
            parse("""{ "aString":"banana" """)
        }.message.isEqualTo("Unterminated object")
        expectThrows<IllegalArgumentException> {
            parse("""{ "aString":"banana",""")
        }.message.isEqualTo("Unterminated object")
    }

    @Test
    fun `badly formed objects`() {
        expectThrows<IllegalArgumentException> {
            parse("{ true: 42 }")
        }.message.isEqualTo("Expected a string key in object not <t>")
        expectThrows<IllegalArgumentException> {
            parse("""{ "key" 42 }""")
        }.message.isEqualTo("Expected a colon in object not <4>")
        expectThrows<IllegalArgumentException> {
            parse("""{ "key" : }""")
        }.message.isEqualTo("Unexpected character in object <}>")
        expectThrows<IllegalArgumentException> {
            parse("""{ "key" : 42,}""")
        }.message.isEqualTo("Expected a string key in object not <}>")
    }

}


