package org.unbrokendome.gradle.plugins.xjc.samples

import org.unbrokendome.gradle.plugins.xjc.testlib.DirectoryBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption


object SamplesIndex {

    private val samplePaths: List<String>


    init {
        samplePaths = checkNotNull(javaClass.getResourceAsStream("/samples-index.txt"))
            .bufferedReader()
            .use { reader ->
                reader.lineSequence()
                    .filter { it.isNotBlank() }
                    .toList()
            }
    }


    fun extractSamples(prefix: String, targetDir: Path) {
        samplePaths.asSequence()
            .filter { it.startsWith(prefix) }
            .forEach { samplePath ->
                checkNotNull(javaClass.getResourceAsStream("/$samplePath")) {
                    "Sample resource does not exist: $samplePath"
                }.use { input ->
                    val targetPath = targetDir.resolve(samplePath.removePrefix(prefix))
                    Files.createDirectories(targetPath.parent)
                    Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
    }
}


fun DirectoryBuilder.extractSamples(prefix: String) {
    SamplesIndex.extractSamples(prefix, path)
}
