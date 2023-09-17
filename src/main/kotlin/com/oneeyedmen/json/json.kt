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
            char == '[' -> ArrayState(this)
            char == '{' -> ObjectState(this)
            char.isWhitespace() -> this
            char == ',' -> Comma(this)
            char == '=' -> Equals(this)
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
    previousState: ParseState?
) : ParseState(previousState) {
    private var isComplete = false
    private var parseState: ParseState = Ground(null)
    override fun accept(char: Char): ParseState =
        when {
            char == ']' && (parseState is Ground || parseState is Literal) -> {
                isComplete = true
                Ground(this)
            }

            else -> {
                parseState = parseState.accept(char)
                this
            }
        }

    override fun value(): List<Any?> =
        when {
            !isComplete -> throw IllegalArgumentException()
            else -> parseState.toSequenceOfStates()
                .filterNot { it is Ground || it is Comma }
                .map { it.value() }
                .toList().reversed()
        }
}

class ObjectState(
    previousState: ParseState?
) : ParseState(previousState) {
    private var isComplete = false
    private var parseState: ParseState = Ground(null)

    override fun accept(char: Char): ParseState =
        when {
            char == '}' && (parseState is Ground || parseState is Literal) -> {
                isComplete = true
                Ground(this)
            }

            else -> {
                parseState = parseState.accept(char)
                this
            }
        }


    override fun value(): Map<String, Any?> =
        when {
            !isComplete -> throw IllegalArgumentException()
            else -> {
                parseState.toSequenceOfStates()
                    .filterNot { it is Ground || it is Comma }
                    .toList().reversed()
                    .windowed(3, 3)
                    .associate { threeStates ->
                        val key = threeStates[0].value() as String
                        val value = threeStates[2].value()
                        key to value
                    }
            }
        }
}

class Equals(previousState: ParseState?) : ParseState(previousState) {
    override fun accept(char: Char): ParseState {
        return Ground(this).accept(char)
    }

    override fun value(): Any? {
        TODO("Not yet implemented")
    }
}

class Comma(previousState: ParseState?) : ParseState(previousState) {
    override fun accept(char: Char): ParseState {
        return Ground(this).accept(char)
    }

    override fun value(): Any? {
        TODO("Not yet implemented")
    }
}

private fun ParseState.toSequenceOfStates() = generateSequence(this) {
    it.previousState
}
