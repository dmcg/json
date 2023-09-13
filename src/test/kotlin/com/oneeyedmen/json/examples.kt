package com.oneeyedmen.json


val aNull = "null"

val aBool = "true"
val anotherBool = "false"
val bools = listOf(aBool, anotherBool)

val anInt = "42"
val aFloat = "12.34"
val anENumber = "12.34E5"
val aNegative = "-12.34"
val numbers = listOf(
    anInt,
    aFloat,
    anENumber,
    aNegative,
)

val aString = "\"banana\""
val anEmptyString = "\"\""
val aBlankString = "\"  \""
val aStringWithASpace = "\"Hello world\""
val aStringWithAQuote = "\"Hello \\\"world\""
val strings = listOf(
    aString,
    anEmptyString,
    aBlankString,
    aStringWithASpace,
    aStringWithAQuote,
    // TODO("other escapes")
)

val anEmptyArray = "[]"
val anotherEmptyArray = "[   ]"
val anEmptyArrayWithALineBreak = "[\n]"
val anArrayWithOneItem = "[ \"banana\" ]"
val anArrayWithOneItemAndNoSpace = "[\"banana\"]"
val anArrayWithTwoItems = "[ \"banana\", null ]"
val anArrayWithTwoItemsOneWithComma = "[ \"Hello, World\", null ]"

val anEmptyObject = "{}"
val anotherEmptyObject = "{  }"
val anEmptyObjectWithALineBreak = "{\n}"
val emptyObjects = listOf(
    anEmptyObject,
    anotherEmptyObject,
    anEmptyObjectWithALineBreak,
)

val aSingleLevelObject = """
    {
        "aNull":null,
        "aBool" : true,
        "aNumber" :-12.34,
        "aString": "banana"
    }
    """.trimIndent()
val aSingleLevelObjectWithNoWhitespace = aSingleLevelObject
    .replace("""\s""".toRegex(), "")
val singleLevelObjects = listOf(
    aSingleLevelObject,
    aSingleLevelObjectWithNoWhitespace,
)

// TODO arrays, multi-level objects