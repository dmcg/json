package com.oneeyedmen.json

import com.oneeyedmen.json.Status.FAIL
import com.oneeyedmen.json.Status.PASS
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThrows
import java.io.File

@Disabled
class JSONTestSuitesTests {

    val dir = File("src/test/resources/test_parsing")

    @Test
    fun test() {
        dir.listFiles().filterNot { file -> file.name == "n_structure_100000_opening_arrays.json" }
            .map { file ->
                testInfo(file)
            }.sortedBy { testInfo -> testInfo.file.name.split('_')[1] }
            .sortedByDescending { testInfo -> testInfo.status }.forEach { testInfo ->
                println(testInfo.toString())
            }
    }


    @Disabled("We fail with StackOverflowError not IllegalArgumentException")
    @Test
    fun `n_structure_100000_opening_arrays`() {
        val file = File(dir, "n_structure_100000_opening_arrays.json")
        val contents = file.readText(Charsets.UTF_8)
        expectThrows<IllegalArgumentException> {
            parse(contents)
        }
    }
}

private fun testInfo(file: File): TestInfo {
    val contents = file.readText(Charsets.UTF_8)
    return try {
        val result = parse(contents)
        val status = if (file.name.startsWith("n_")) FAIL else PASS
        TestInfo(file, contents, status, result.toString())
    } catch (x: Throwable) {
        val result = x.message
        val status = if (file.name.startsWith("y_")) FAIL else PASS
        TestInfo(file, contents, status, result.toString())
    }
}


enum class Status {
    PASS, FAIL
}

data class TestInfo(
    val file: File,
    val contents: String,
    val status: Status,
    val resultAsString: String
) {
    override fun toString() = "$status ${file.name} <${contents.take(100)}> -> <${resultAsString.take(100)}>"
}