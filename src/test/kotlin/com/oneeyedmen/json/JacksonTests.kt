package com.oneeyedmen.json

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import kotlin.random.Random

class JacksonTests {
    val objectMapper = ObjectMapper()

    @Test
    fun mixed() {
        val mixed = listOf("banana", true, false, null, 1, -1, Math.PI.toBigDecimal())
        checkAll(mixed)
    }

    @Test
    fun strings() {
        checkAll(someStrings)
    }

    @Test
    fun numbers() {
        checkAll(someNumbers)
    }

    @Test
    fun booleans() {
        checkAll(someBooleans)
    }

    @Test
    fun `maps in a list`() {
        val input: List<Map<String, Any?>> = listOf(someBooleans, someStrings, someNumbers).map { it.toMapWithNames() }
        check(input)
    }

    @Test
    fun `lists in a map`() {
        val input: Map<String, List<Any?>> = listOf(someBooleans, someStrings, someNumbers).toMapWithNames()
        check(input)
    }

    @Test
    fun `lists in a list`() {
        val input: List<List<Any?>> = listOf(listOf(someBooleans, someStrings), someNumbers)
        check(input)
    }

    @Test
    fun `maps in a map`() {
        val input: Map<String, Map<String, Any?>> = listOf(someBooleans, someStrings, someNumbers)
            .map { it.toMapWithNames() }
            .toMapWithNames()
        check(input)
    }

    @Test
    fun `key names`() {
        val input: Map<String, Any?> = someKeys.zip(someStrings + someNumbers).toMap()
        check(input)
    }

    private val randomSeed = System.currentTimeMillis()

    private val random = Random(randomSeed)

    @RepeatedTest(100)
    fun `random lists`() {
        println("Random seed : $randomSeed")
        val input = randomList()
        check(input)
    }

    @RepeatedTest(100)
    fun `random maps`() {
        println("Random seed : $randomSeed")
        val input = randomList().toMapWithNames()
        check(input)
    }

    private fun randomList(): List<Any?> {
        val elementCount = listOf(0, 1, 1, 2, 2, 2, 7).random(random)
        return List(elementCount) {
            randomThing()
        }
    }

    private fun randomThing() = when (random.nextInt(5)) {
        0 -> randomList()
        1 -> randomList().toMapWithNames()
        else -> listOf(someBooleans, someStrings, someNumbers).flatten().random(random)
    }

    private fun checkAll(things: List<Any?>) {
        for (each in things) {
            check(each)
        }
        for (each in things) {
            check(listOf(each))
        }
        check(things)
        check(things.toMapWithNames())
    }

    private fun check(input: Any?) {
        val json = objectMapper.writeValueAsString(input)
        val parsedResult = parse(json)
        expectThat(parsedResult).isEqualTo(input)
    }
}

private val someBooleans = listOf(true, false)
private val someStrings = listOf("", " ", "\t", "hello\nworld", "hello\u0000world", null)
private val someNumbers = listOf(
    0, -1, 1, Int.MAX_VALUE, Int.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE,
    0.0.toBigDecimal(), Math.PI.toBigDecimal(), Math.PI.toBigDecimal() * BigDecimal("1E100")
)

private val someKeys = listOf("key", "with\tatab", "with a space", "with\nnewline", "", "\u0000", "\u0001", "\uffff")

private fun <T> List<T>.toMapWithNames(): Map<String, T> =
    mapIndexed { index, each -> "thing-$index" to each }.toMap()

