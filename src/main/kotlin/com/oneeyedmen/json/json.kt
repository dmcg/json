package com.oneeyedmen.json

fun parse(json: CharSequence): Any? {
    var state: ParseState = TopLevelGround
    val values = mutableListOf<Any?>()
    json.forEach { char ->
        val newState = state.accept(char)
        if (newState != state) {
            if (values.isNotEmpty())
                throw IllegalArgumentException("Cannot have more than one top level result, failed at <$char>")
            else
                values.addValueFrom(state)
        }
        state = newState
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
            char == ',' -> Comma
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
    private var isComplete = false
    private var parseState: ParseState = Ground
    private val states = mutableListOf<ParseState>()
    override fun accept(char: Char): ParseState =
        when {
            char == ']' && (parseState is Ground || parseState is Literal) -> {
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

    override fun value(): List<Any?> =
        when {
            !isComplete -> throw IllegalArgumentException()
            else -> states
                .filterIsInstance<Valued>()
                .map { it.value() }
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

private object Comma : ParseState() {
    override fun accept(char: Char) = Ground.accept(char)
}
