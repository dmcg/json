package com.oneeyedmen.json

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JacksonTests {
    val objectMapper = ObjectMapper()

    @Test
    fun `round trip`() {
        val input = listOf("banana", true, false, null, 1, -1, Math.PI.toBigDecimal())
        val json = objectMapper.writeValueAsString(input)
        println(json)
        val parsedResult = parse(json)
        expectThat(parsedResult).isEqualTo(input)
    }
}