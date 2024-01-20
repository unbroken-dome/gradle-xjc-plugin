package org.unbrokendome.gradle.plugins.xjc.testutil

import org.junit.jupiter.api.Assertions.assertNotNull
import java.math.BigDecimal

class JavaVersionUtil {

    companion object {
        fun javaVersionAtLeast(minimumVersion: Int): Boolean {
            val javaVersion = System.getProperty("java.version")?.split(".")?.subList(0, 2)?.joinToString(".")    // "1.8" => "1.8"
            assertNotNull(javaVersion)
            return BigDecimal(javaVersion) >= BigDecimal(minimumVersion)
        }
    }

}
