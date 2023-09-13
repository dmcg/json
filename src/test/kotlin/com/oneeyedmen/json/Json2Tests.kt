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

private fun parse(json: String): Any? {
    var state: ParseState = Ground(null)
    json.forEach { char ->
        state = state.accept(char)
    }
    return state.value()
}


abstract class ParseState(val previousState: ParseState?) {
    abstract fun accept(char: Char): ParseState
    abstract fun value(): Any?
}

class Ground(previousState: ParseState?) : ParseState(previousState) {
    override fun accept(char: Char): ParseState =
        when {
            char == '"' -> StringState(this, char)
            char == '[' -> ArrayState(this, char)
            char.isWhitespace() -> this
            char == ',' -> this
            else -> Literal(this, char)
        }

    override fun value(): Any? =
        when (previousState) {
            null -> throw IllegalArgumentException()
            else -> previousState.value()
        }
}

class Literal(
    previousState: ParseState?,
    char: Char
) : ParseState(previousState) {
    private val chars = StringBuilder().append(char)

    override fun accept(char: Char): ParseState {
        return when {
            char.isWhitespace() -> Ground(this)
            char == ',' -> Ground(this)
            else -> {
                chars.append(char)
                this
            }
        }
    }

    override fun value(): Any? =
        when (val string = chars.toString()) {
            "null" -> null
            "true" -> true
            "false" -> false
            else -> string.toBigDecimalOrNull() ?: throw IllegalArgumentException("Not a literal <$string>")
        }

}

class StringState(
    previousState: ParseState?,
    char: Char
) : ParseState(previousState) {
    private val chars = StringBuilder().append(char)
    override fun accept(char: Char): ParseState =
        when (char) {
            '\"' -> {
                if (chars.endsWith('\\')) {
                    chars.deleteCharAt(chars.length - 1).append(char)
                    this
                } else {
                    chars.append(char)
                    Ground(this)
                }
            }

            else -> {
                chars.append(char)
                this
            }
        }

    override fun value(): String =
        when {
            chars.last() != '\"' -> throw IllegalArgumentException()
            else -> chars.substring(1, chars.length - 1)
        }
}

class ArrayState(
    previousState: ParseState?,
    char: Char
) : ParseState(previousState) {
    private val chars = StringBuilder().append(char)
    private val elements = mutableListOf<ParseState>()
    private var parseState: ParseState = Ground(null)
    override fun accept(char: Char): ParseState =
        when  {
            char == ']' && parseState is Ground -> {
                chars.append(char)
                elements.add(parseState)
                Ground(this)
            }

            else -> {
                val newParseState = parseState.accept(char)
                if (parseState != newParseState)
                    elements.add(parseState)
                parseState = newParseState
                chars.append(char)
                this
            }
        }

    override fun value(): List<Any?> =
        when {
            chars.last() != ']' -> throw IllegalArgumentException()
            elements.isEmpty() -> emptyList()
            elements.size == 1 && elements.first() is Ground -> emptyList()
            else -> elements.filterNot { it is Ground }.map { it.value() }
        }
}
