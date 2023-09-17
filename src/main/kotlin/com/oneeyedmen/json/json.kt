package com.oneeyedmen.json

fun parse(json: String): Any? {
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