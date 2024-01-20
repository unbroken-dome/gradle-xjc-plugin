package org.unbrokendome.gradle.plugins.xjc

import org.gradle.internal.impldep.org.testng.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.xjc.XjcSourceSetConvention.Companion.toWords

object MaintenanceChangesTest : Spek({

    describe("org.gradle.util.GUtil deprecation") {
        it("validate replacement method") {
            val expected = sequenceOf(
                "" to "",
                "a" to "a",
                "A_Z" to "a z",
                "oneTwo" to "one two",
                "A9_Z" to "a9 z",
                "A_9Z" to "a 9 z",
                "A\$9Z4" to "a 9 z4",
                "0A\$9Z4" to "0 a 9 z4",
                "one9Two" to "one9 two",
                "UPPERCASE" to "uppercase",
                "UPPERcase" to "uppe rcase",    // this is what came out of original method
                "upperCASE" to "upper case",
                "ABbCddE" to "a bb cdd e",
                "ABbCDdE" to "a bb c dd e",
                "AbbCddE" to "abb cdd e",
                "AbbCdE" to "abb cd e",
                "AbCdE" to "ab cd e",
                "abcDefGhi Jkl mnoPqr stu vwx yz" to "abc def ghi jkl mno pqr stu vwx yz",
                "A\$B?C-D:E.F" to "a b c d e f"
            )

            expected.forEach {
                val actual = toWords(it.first)
                //val actual = org.gradle.util.GUtil.toWords(it.first)
                assertEquals(actual, it.second, "'${it.first}' to actual '$actual' != '${it.second}' expected")
            }
        }
    }

})
