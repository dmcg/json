package com.oneeyedmen.json

fun parse(json: CharSequence): Any? {
    var state: ParseState = SeekingValue
    val values = ArrayList<Any?>(1)
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
        values.isEmpty() -> throw IllegalArgumentException("No top-level content found")
        values.size != 1 -> error("Unexpectedly found more than one top level value")
        else -> values.single()
    }
}

private interface ParseState {
    fun accept(char: Char): ParseState
}

private interface Valued {
    fun value(): Any?
}

private object SeekingValue : ParseState {
    override fun accept(char: Char): ParseState =
        when {
            char == '"' -> StringState(this, char)
            char == '[' -> ArrayState(this)
            char == '{' -> ObjectState(this)
            char.isJSONWhitespace() -> this
            char.isValidInLiteral() -> Literal(this, char)
            else -> throw IllegalArgumentException("Not a valid top-level character <$char>")
        }
}

private fun Char.isValidInLiteral() = "nulltruefalse0123456789-+.eE".contains(this)

private class Literal(
    val ground: ParseState,
    char: Char
) : ParseState, Valued {
    private val chars = StringBuilder().append(char)

    override fun accept(char: Char) = when {
        char == ',' -> ground.accept(char)
        char == ':' -> ground.accept(char)
        char.isJSONWhitespace() -> ground.accept(char)
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
            else -> if (string.matches(numberRegex))
                string.toBigDecimalOrNull() ?: throw IllegalArgumentException("Not a literal <$string>")
            else
                throw IllegalArgumentException("Not a valid number <$string>")
        }

}

private val numberRegex = """^-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?$""".toRegex()

private class StringState(
    val ground: ParseState,
    char: Char
) : ParseState, Valued {
    private val chars = StringBuilder().append(char)
    private var state: ParseState = Unescaped()

    override fun accept(char: Char): ParseState =
        when (val newState = state.accept(char)) {
            ground -> ground
            else -> {
                state = newState
                this
            }
        }

    override fun value(): String =
        when {
            chars.last() != '\"' ->
                throw IllegalArgumentException("Unterminated string <$chars>")

            else -> chars.substring(1, chars.length - 1)
        }

    inner class Unescaped : ParseState {
        override fun accept(char: Char) = when (char) {
            '\\' -> Escaped()
            '\"' -> {
                chars.append(char)
                ground
            }

            else -> {
                if (char.isJSONControlCharacter()) throw IllegalArgumentException("Illegal character with code <${char.code}> in string")
                chars.append(char)
                this
            }
        }
    }

    inner class Escaped : ParseState {
        override fun accept(char: Char) = when (char) {
            'u' -> Unicode()
            else -> {
                when (char) {
                    '\"' -> chars.append(char)
                    '\\' -> chars.append(char)
                    '/' -> chars.append(char)
                    'b' -> chars.append('\b')
                    'n' -> chars.append('\n')
                    'r' -> chars.append('\r')
                    't' -> chars.append('\t')
                    'f' -> chars.append('\u000c')
                    else -> throw IllegalArgumentException("Illegal escape <\\$char>")
                }
                Unescaped()
            }
        }
    }

    inner class Unicode : ParseState {
        val digits = StringBuilder()
        override fun accept(char: Char) = when (char) {
            in "0123456789abcdefABCDEF" -> {
                digits.append(char)
                if (digits.length == 4) {
                    chars.append(digits.toChar())
                    Unescaped()
                } else this
            }

            else -> {
                val escape = if (char == '\"' || char.isJSONWhitespace()) "$digits" else "$digits$char"
                throw IllegalArgumentException("Illegal unicode escape <\\u$escape>")
            }
        }
    }
}

private fun Char.isJSONControlCharacter() = this in '\u0000'..'\u001f'

private fun StringBuilder.toChar(): Char = this.toString().toInt(16).toChar()

private class ArrayState(val ground: ParseState) : ParseState, Valued {
    private var state: ParseState = SeekingValue
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
            (state is SeekingValue && values.isEmpty())

    override fun value(): List<Any?> = when {
        !isComplete -> throw IllegalArgumentException("Unterminated array")
        else -> values
    }

    private object SeekingValue : ParseState {
        override fun accept(char: Char): ParseState = when {
            char == '"' -> StringState(SeekingComma, char)
            char == '[' -> ArrayState(SeekingComma)
            char == '{' -> ObjectState(SeekingComma)
            char.isJSONWhitespace() -> this
            char.isValidInLiteral() -> Literal(SeekingComma, char)
            else -> throw IllegalArgumentException("Unexpected character in array <$char>")
        }
    }

    private object SeekingComma : ParseState {
        override fun accept(char: Char): ParseState = when {
            char == ',' -> SeekingValue
            char.isJSONWhitespace() -> this
            else -> throw IllegalArgumentException("Expected a comma in an array, got <$char>")
        }
    }
}

private class ObjectState(val ground: ParseState) : ParseState, Valued {
    private var state: ParseState = SeekingKey
    private val values = mutableListOf<Any?>()
    private var isComplete = false

    override fun accept(char: Char): ParseState =
        when {
            char == '}' && objectIsCompletable() -> {
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

    override fun value(): Map<String, Any?> =
        when {
            !isComplete -> throw IllegalArgumentException("Unterminated object")
            else -> {
                values
                    .windowed(2, 2, partialWindows = true)
                    .associate { keyAndValue ->
                        if (keyAndValue.size != 2)
                            error("Didn't get both a key and value, only <${keyAndValue[0]}>")
                        val key =
                            (keyAndValue[0] as? String) ?: error("Key in object <${keyAndValue[0]}> is not a string")
                        val value = keyAndValue[1]
                        key to value
                    }
            }
        }

    private fun objectIsCompletable() =
        state is Literal ||
            state is SeekingComma ||
            (state is SeekingKey && values.isEmpty())

    private object SeekingKey : ParseState {
        override fun accept(char: Char): ParseState =
            when {
                char == '"' -> StringState(SeekingColon, char)
                char.isJSONWhitespace() -> this
                else -> throw IllegalArgumentException("Expected a string key in object not <$char>")
            }
    }

    private object SeekingColon : ParseState {
        override fun accept(char: Char): ParseState =
            when {
                char == ':' -> SeekingValue
                char.isJSONWhitespace() -> this
                else -> throw IllegalArgumentException("Expected a colon in object not <$char>")
            }
    }

    private object SeekingValue : ParseState {
        override fun accept(char: Char): ParseState =
            when {
                char == '"' -> StringState(SeekingComma, char)
                char == '[' -> ArrayState(SeekingComma)
                char == '{' -> ObjectState(SeekingComma)
                char.isJSONWhitespace() -> this
                char.isValidInLiteral() -> Literal(SeekingComma, char)
                else -> throw IllegalArgumentException("Unexpected character in object <$char>")
            }
    }

    private object SeekingComma : ParseState {
        override fun accept(char: Char): ParseState =
            when {
                char == ',' -> SeekingKey
                char.isJSONWhitespace() -> this
                else -> throw IllegalArgumentException("Expected a comma in object not <$char>")
            }
    }
}

private fun MutableList<Any?>.addValueFrom(state: ParseState) {
    (state as? Valued)?.let { add(it.value()) }
}

private fun Char.isJSONWhitespace() = this == '\u0020' || this == '\u0009' || this == '\u000A' || this == '\u000D'
