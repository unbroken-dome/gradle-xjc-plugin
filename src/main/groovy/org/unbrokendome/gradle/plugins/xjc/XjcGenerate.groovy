package org.unbrokendome.gradle.plugins.xjc

import com.sun.codemodel.JCodeModel
import com.sun.tools.xjc.Language
import com.sun.tools.xjc.ModelLoader
import com.sun.tools.xjc.Options
import com.sun.tools.xjc.api.SpecVersion
import com.sun.tools.xjc.util.ErrorReceiverFilter
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

class XjcGenerate extends SourceTask {

    @OutputDirectory
    File outputDirectory

    @InputFiles @Optional
    FileCollection bindingFiles

    @InputFiles @Optional
    FileCollection catalogs

    @InputFiles @Optional
    FileCollection episodes

    @InputFiles @Optional
    FileCollection classpath

    @Input @Optional
    String targetVersion
    @Input
    boolean extension = false
    @Input
    boolean strictCheck = true
    @Input
    boolean packageLevelAnnotations = true
    @Input
    boolean noFileHeader = true
    @Input
    boolean enableIntrospection = false
    @Input
    boolean contentForWildcard = false
    @Input
    String encoding = 'UTF-8'
    @Input
    boolean readOnly = false
    @Console
    boolean quiet = false
    @Console
    boolean verbose = false


    @OutputFile @Optional
    File episodeTargetFile

    @Input @Optional
    List<String> extraArgs

    @Input @Optional
    String targetPackage


    @TaskAction
    void generateCode() {

        Options options = buildOptions()

        def listener = new LoggingXjcListener(logger)
        def errorReceiver = new ErrorReceiverFilter(listener)

        def model = ModelLoader.load(options, new JCodeModel(), errorReceiver)
        if (model == null) {
            throw new Exception('Parse failed')
        }

        def outline = model.generateCode(options, errorReceiver)
        if (outline == null) {
            throw new Exception('Code generation failed')
        }

        listener.compiled(outline)

        def codeWriter = options.createCodeWriter()
        model.codeModel.build(codeWriter)
    }


    private Options buildOptions() {
        def options = new Options()

        options.schemaLanguage = Language.XMLSCHEMA
        options.target = (targetVersion != null) ? SpecVersion.parse(targetVersion) : SpecVersion.LATEST
        options.targetDir = getOutputDirectory()

        source?.each { options.addGrammar it }
        bindingFiles?.each { options.addBindFile it }
        catalogs?.each { options.addCatalog it }
        classpath?.each { options.classpaths.add it.toURI().toURL() }
        episodes?.each { options.scanEpisodeFile it }

        options.strictCheck = strictCheck
        options.packageLevelAnnotations = packageLevelAnnotations
        options.noFileHeader = noFileHeader
        options.enableIntrospection = enableIntrospection
        options.contentForWildcard = contentForWildcard
        options.defaultPackage = targetPackage
        options.encoding = encoding
        options.readOnly = readOnly
        options.quiet = quiet
        options.verbose = verbose

        if (extension || extraArgs?.any { it.startsWith('-X')}) {
            options.compatibilityMode = Options.EXTENSION
        }

        if (episodeTargetFile) {
            options.parseArguments(['-episode', episodeTargetFile.absolutePath] as String[])
        }

        if (extraArgs) {
            options.parseArguments(extraArgs as String[])
        }

        options
    }
}
