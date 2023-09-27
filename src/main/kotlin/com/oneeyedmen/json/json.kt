package com.oneeyedmen.json

fun parse(json: CharSequence): Any? {
    val initialState = Ground
    var state: ParseState = initialState
    val values = mutableListOf<Any?>()
    json.forEach { char ->
        val newState = state.accept(char)
        if (newState != state && (newState !is Ground && values.isNotEmpty()) || newState is Comma)
            throw IllegalArgumentException("Cannot have more than one top level result, failed at <$char>")
        if (newState != state && state is Valued) {
            val oldState = state as Valued
            values.add(oldState.value())
        }
        state = newState
    }
    if (state is Valued) {
        val valueState = state as Valued
        values.add(valueState.value())
    }
    return when {
        values.isEmpty() -> throw IllegalArgumentException()
        else -> values.single()
    }
}

interface Valued {
    fun value(): Any?
}

private abstract class ParseState {
    abstract fun accept(char: Char): ParseState
}

private object Ground : ParseState() {
    override fun accept(char: Char): ParseState =
        when {
            char == '"' -> StringState(char)
            char == '[' -> ArrayState()
            char == '{' -> ObjectState()
            char == ',' -> Comma
            char == ':' -> Colon
            char.isWhitespace() -> this
            else -> Literal(char)
        }
}

private class Literal(
    char: Char
) : ParseState(), Valued {
    private val chars = StringBuilder().append(char)

    override fun accept(char: Char) = when {
        char == ',' -> Comma
        char == ':' -> Colon
        char.isWhitespace() -> Ground
        else -> {
            chars.append(char)
            this
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

private class StringState(
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
                    Ground
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

private class ArrayState : ParseState(), Valued {
    private var isComplete = false
    private var parseState: ParseState = Ground
    private val states = mutableListOf<ParseState>()
    override fun accept(char: Char): ParseState =
        when {
            char == ']' && (parseState is Ground || parseState is Literal) -> {
                isComplete = true
                Ground
            }

            else -> {
                parseState = parseState.accept(char)
                if (states.lastOrNull() != parseState)
                    states.add(parseState)
                this
            }
        }

    override fun value(): List<Any?> =
        when {
            !isComplete -> throw IllegalArgumentException()
            else -> states
                .filterIsInstance<Valued>()
                .map { it.value() }
        }
}

private class ObjectState : ParseState(), Valued {
    private var isComplete = false
    private var parseState: ParseState = Ground
    private val states = mutableListOf<ParseState>()

    override fun accept(char: Char): ParseState =
        when {
            char == '}' && (parseState is Ground || parseState is Literal) -> {
                isComplete = true
                Ground
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

private object Comma : ParseState() {
    override fun accept(char: Char) = Ground.accept(char)
}
