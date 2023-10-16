package com.oneeyedmen.json

fun parse(json: CharSequence): Any? {
    var state: ParseState = TopLevelGround
    val values = mutableListOf<Any?>()
    json.forEach { char ->
        state = state.accept(char).also { newState ->
            if (newState != state) {
                if (values.isNotEmpty())
                    throw IllegalArgumentException("Cannot have more than one top level result, failed at <$char>")
                else
                    values.addValueFrom(state)
            }
        }
    }
    values.addValueFrom(state)
    return when {
        values.isEmpty() -> throw IllegalArgumentException()
        else -> values.single()
    }
}

private fun MutableList<Any?>.addValueFrom(state: ParseState) {
    (state as? Valued)?.let { add(it.value()) }
}

interface Valued {
    fun value(): Any?
}

private abstract class ParseState {
    abstract fun accept(char: Char): ParseState
}

private object TopLevelGround : ParseState() {
    override fun accept(char: Char): ParseState =
        when {
            char == '"' -> StringState(this, char)
            char == '[' -> ArrayState(this)
            char == '{' -> ObjectState(this)
            char.isWhitespace() -> this
            char.isValidInLiteral() -> Literal(this, char)
            else -> throw IllegalArgumentException("Not a valid top-level character <$char>")
        }
}

private fun Char.isValidInLiteral() = "nulltruefalse0123456789-+.eE".contains(this)

private object Ground : ParseState() {
    override fun accept(char: Char): ParseState =
        when {
            char == '"' -> StringState(this, char)
            char == '[' -> ArrayState(this)
            char == '{' -> ObjectState(this)
            char == ',' -> Comma(this)
            char == ':' -> Colon
            char.isWhitespace() -> this
            else -> Literal(this, char)
        }
}

private class Literal(
    val ground: ParseState,
    char: Char
) : ParseState(), Valued {
    private val chars = StringBuilder().append(char)

    override fun accept(char: Char) = when {
        char == ',' -> ground.accept(char)
        char == ':' -> ground.accept(char)
        char.isWhitespace() -> ground
        char.isValidInLiteral() -> {
            chars.append(char)
            this
        }

        else -> throw IllegalArgumentException("Illegal literal character <$char>")
    }

    override fun value(): Any? =
        when (val string = chars.toString()) {
            "null" -> null
            "true" -> true
            "false" -> false
            else -> string.toBigDecimalOrNull() ?: throw IllegalArgumentException("Not a literal <$string>")
        }

}

private class StringState(
    val ground: ParseState,
    char: Char
) : ParseState(), Valued {
    private val chars = StringBuilder().append(char)
    override fun accept(char: Char): ParseState =
        when (char) {
            '\"' -> {
                if (chars.endsWith('\\')) {
                    chars.deleteCharAt(chars.length - 1).append(char)
                    this
                } else {
                    chars.append(char)
                    ground
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

private class ArrayState(val ground: ParseState) : ParseState(), Valued {
    private var state: ParseState = ArrayGround
    private val values = mutableListOf<Any?>()
    private var isComplete = false

    override fun accept(char: Char): ParseState =
        when {
            char == ']' && (arrayIsCompletable()) -> {
                isComplete = true
                values.addValueFrom(state)
                ground
            }

            else -> {
                state = state.accept(char).also { newState ->
                    if (newState != state) {
                        values.addValueFrom(state)
                    }
                }
                this
            }
        }

    private fun arrayIsCompletable() =
        state is Literal ||
            state is SeekingComma ||
            (state is ArrayGround && values.isEmpty())

    override fun value(): List<Any?> = when {
        !isComplete -> throw IllegalArgumentException("Unterminated array")
        else -> values
    }

    private object ArrayGround : ParseState() {
        override fun accept(char: Char): ParseState = when {
            char == '"' -> StringState(SeekingComma, char)
            char == '[' -> ArrayState(SeekingComma)
            char == '{' -> ObjectState(SeekingComma)
            char.isWhitespace() -> this
            char.isValidInLiteral() -> Literal(SeekingComma, char)
            else -> throw IllegalArgumentException("Unexpected character in array <$char>")
        }
    }

    private object SeekingComma : ParseState() {
        override fun accept(char: Char): ParseState = when {
            char == ',' -> ArrayGround
            char.isWhitespace() -> this
            else -> throw IllegalArgumentException("Expected a comma in an array, got <$char>")
        }
    }
}

private class ObjectState(val ground: ParseState) : ParseState(), Valued {
    private var isComplete = false
    private var parseState: ParseState = Ground
    private val states = mutableListOf<ParseState>()

    override fun accept(char: Char): ParseState =
        when {
            char == '}' && (parseState is Ground || parseState is Literal) -> {
                isComplete = true
                ground
            }

            else -> {
                parseState = parseState.accept(char)
                if (states.lastOrNull() != parseState)
                    states.add(parseState)
                this
            }
        }


    override fun value(): Map<String, Any?> =
        when {
            !isComplete -> throw IllegalArgumentException()
            else -> {
                states
                    .filterIsInstance<Valued>()
                    .windowed(2, 2)
                    .associate { threeStates ->
                        val key = threeStates[0].value() as String
                        val value = threeStates[1].value()
                        key to value
                    }
            }
        }
}

private object Colon : ParseState() {
    override fun accept(char: Char) = Ground.accept(char)
}

private class Comma(val ground: ParseState) : ParseState() {
    override fun accept(char: Char) = ground.accept(char)
}
