# Gradle XJC Plugin

Invokes the `xjc` binding compiler from a Gradle build.


## Usage

### Applying the plugin

To use the XJC plugin, include either of the following in your build script:

#### New Plugins DSL (Gradle 2.1+)

```groovy
plugins {
    id 'org.unbroken-dome.xjc' version '1.4.2'
}
```

#### Traditional (Gradle 1.x/2.0)

```groovy
buildscript {
    repositories { jcenter() }
    dependencies {
        classpath 'org.unbroken-dome.gradle-plugins:gradle-xjc-plugin:1.4.2'
    }
}

apply plugin: 'org.unbroken-dome.xjc'
```


## Configuration

Just by applying the plugin, your project will now a task named `xjcGenerate` with sensible defaults - if these
 work for you, then you can just run `gradle build` and the `xjc` tool will run and generate code for you.

The parameters to the `xjcGenerate` task correspond to the parameters to the
 [`xjc` command line tool](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/xjc.html), but use
 the Gradle constructs (e.g. source sets) where it is more comfortable. The following table lists some common parameters
 to the `xjcGenerate` task.

### Example

```groovy
xjcGenerate {
    source = fileTree('src/main/schema') { include '*.xsd' }
    bindingFiles = fileTree('src/main/jaxb') { include '*.xjb' }
    catalogs = fileTree('src/main/catalog') { include '*.cat' }
}
```


### Common Parameters

| Parameter | Type | Command-line equivalent | Default / convention value |
|---|---|---|---|
| `source` | `FileTree` | | all `*.xsd` files under `src/main/schema` |
| `bindingFiles` | `FileCollection` | `-b` | all `*.xjb` files under `src/main/schema` |
| `outputDirectory` | `File` | `-d` | `build/xjc/generated-sources` |


### Additional Parameters

The following parameters are used less commonly, to fine-tune the generation process or
 in special scenarios. Please refer to the
 [`xjc` command line tool](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/xjc.html)
 documentation for their exact meaning.

| Parameter | Type | Command-line equivalent | Default / convention value |
|---|---|---|---|
| `catalogs` | `FileCollection` | `-catalog` | not set |
| `episodes` | `File` | | all modules in the `xjcEpisode` configuration |
| `episodeTargetFile` | `File` | `-episode` | `build/xjc/sun-jaxb.episode` |
| `extension` | `boolean` | `-extension` | `false` |
| `packageLevelAnnotations` | `boolean` | `-npa` (`true` if not present) | `true` |
| `pluginClasspath` | `FileCollection` | `-classpath` | all modules in the `xjcClasspath` configuration |
| `quiet` | `boolean` | `-quiet` | `false` |
| `readOnly` | `boolean` | `-readOnly` | `false` |
| `strictCheck` | `boolean` | `-nv` (`true` if not present) | `true` |
| `targetPackage` | `String` | `-p` | not set |
| `targetVersion` | `String` | `-target` | use latest version  |
| `verbose` | `boolean` | `-verbose` | `false` |

The following parameters are specific to the Gradle plugin and have no command-line equivalent:

| Parameter | Type | Description | Default value |
|---|---|---|---|
| `catalogResolutionClasspath` | `Configuration` | Where to look for imported schemas when using the `maven:` or `classpath:` URI scheme in catalogs (see below) | `compileClasspath` (`compile` in older Gradle versions) |
| `urlSources` | `FileCollection` | Files containing URLs of remote schemas (see below) | All `*.url` files under `src/main/schema` |

Additionally, the following parameter may be used to control the output code generation:

| Parameter | Type | Description | Default value |
|---|---|---|---|
| `encoding` | `String` | Encoding for generated Java files | `UTF-8` |
| `docLanguage` | `String` | Desired language for Javadoc comments in generated code (e.g. `"en-US"`) | JVM's default `Locale` |

## Remote schemas

If the schema cannot or should not be located in the local file system, place a file with the extension `.url` into
`src/main/schema` which contains the URL of the remote schema (a WSDL for example).

Multiple URLs can be placed into one file, one URL per line. Alternativly multiple files with one line each can be used.

The location of such URL listing files can be configured using the `urlSources` property of the `xjcGenerate` task.

#### Example

```text
# remote.url
https://www.domain.com/service?WSDL
```

```groovy
xjcGenerate {
    urlSources = fileTree('src/main/schema') { include '*.url' }
}
```


## Including Generated Code in the Compilation

By default, the output directory of the code generation is added to the `main` source set, and the `xjcGenerate` task
 is added as a dependency of `compileJava`. That means that code generation will happen first, and the two sets of
 source code (generated and hand-written code) will be compiled together. This is the behavior that most users will
 expect.

You can switch off this behavior by setting `xjc.includeInMainCompilation` to `false`. In this case, the `xjcGenerate`
 task is available, but not automatically run with a `gradle build`.

### Using a separate source set

To place the generated code in a different source set, include the task's output directory in it:

```groovy
xjc {
    includeInMainCompilation = false
}

sourceSets {
    generated { srcDir xjcGenerate.outputDirectory }
}
```


## Using XJC plugins

XJC allows to hook into and extend the code-generation process by using plugins. A plugin might, for example, add
 `equals()` and `toString()` methods into generated classes.

### Specifying the plugin classpath

The plugin JARs must be on the classpath for the `xjc` invocation. With the xjc Gradle plugin, you can do this very
 comfortably by adding dependencies to the `xjcClasspath` configuration:

```groovy
dependencies {
    xjcClasspath 'org.jvnet.jaxb2_commons:jaxb2-basics:0.11.1'
}
```

### Specifying extra arguments

Most XJC plugins need to be activated and/or configured using command-line arguments. You can specify these extra
 arguments in the `extraArgs` parameter to the `xjcGenerate` task:

```groovy
xjcGenerate {
    extraArgs = [
        '-Xcommons-lang',
        '-Xfluent-api'
    ]
}
```

The plugin will automatically use the JAXB extension mode if there are parameters starting with `-X`
 present, regardless of the `extension` flag.


## Using Episodes

An episode file is a special file that contains information about the generated classes and can be imported by
 subsequent xjc runs to re-use the generated code. If the episode file is placed in the "magic" location
 `META-INF/sun-jaxb.episode` inside a JAR file, it will be picked up automatically by dependent builds. This is
 convenient when an import hierarchy of XML schemas should be reflected in a dependency hierarchy of JARs.

### Importing Episodes

To specify episodes to be imported into the xjc build, add the libraries containing them to the `xjcEpisode`
 configuration:

```groovy
dependencies {
    xjcEpisode 'org.example:my-model:1.2.3'
}
```

These dependencies should resolve to JAR files; if they contain a `META-INF/sun-jaxb.episode` entry it will be
 imported by the current `xjc` invocation.

Of course, you may use a different configuration, like `compileClasspath`, as the source of episode JARs:

```groovy
xjcGenerate {
    episodes compileClasspath
}
```


### Including the Episode File in the JAR

By default, an episode file will be generated under `build/xjc/sun-jaxb.episode`, but it will not be included in the
 build's JAR artifact. If you would like to do so, set the `xjc.includeEpisodeFileInJar` flag to `true`:

```groovy
xjc {
    includeEpisodeFileInJar = true
}
```

which would place the episode file under `META-INF/sun-jaxb.episode` in the JAR.


## Working with Catalogs

Catalogs can be used to map the URI of an imported schema (specified using `<xsd:import>`) to an actual URL or file
 from where it can be read. This is especially useful if the imported URI is only symbolic, or you cannot
 (or do not want to) change the importing schema.

To use a catalog, first specify the location of the catalog file(s) in the `XjcGenerate` task configuration,
 for example:

```groovy
xjcGenerate {
    catalogs = fileTree('src/main/catalog') { include '*.cat' }
}
```

In the catalog file you can use the `REWRITE_SYSTEM` instruction to map an URI to the actual location of the schema,
 e.g.

```text
REWRITE_SYSTEM "http://schemas.example.com" "http://www.example.com/etc/schemas/"
```

### The `classpath:` and `maven:` URI Schemes

This plugin supports two special URI schemes in catalogs, the `classpath:` and `maven:` scheme. They are inspired
 (and largely compatible) with the [JAXB2 Maven Plugin](https://github.com/highsource/maven-jaxb2-plugin), in order
 to simplify a migration from Maven to Gradle in projects that use XJC code generation.

The `classpath:` interprets the rest of the URI as the path to a classpath resource. This is especially useful for
 multi-step code generation where a library JAR contains the schema, an episode file and generated code:

```groovy
// build.gradle

dependencies {
    implementation 'com.example:my-model:1.2.3'
}

xjcGenerate {
    episodes compileClasspath         // compileClasspath includes all "implementation" dependencies
}
```

And assuming that the `my-model` JAR contains an XSD resource at `schemas/my-model.xsd`, you could write
the catalog file as follows:

```text
# catalog file
REWRITE_SYSTEM "http://schemas.example.com/" "classpath:schemas/"
```

And then reference it in the importing schema:

```xml
<!-- The schemaLocation will be mapped to the JAR classpath resource thanks to the catalog -->
<xsd:import namespace="http://schemas.example.com/mymodel"
            schemaLocation="http://schemas.example.com/my-model.xsd" />
```

By default, all JARs in the special configuration `xjcCatalogResolution` are taken into account, which
 inherits all dependencies from `compileClasspath` (unless `includeInMainCompilation` is set to `false` -
 see below). You can add dependencies to this configuration, or use a custom configuration by setting
 the `catalogResolutionClasspath` property on the `XjcGenerate` task.

When using catalogs together with a separate source set (i.e. setting `includeInMainCompilation` to false),
you will need to specify the catalog resolution dependencies manually. The reason for this is that usually your
`compileClasspath` will include the XJC-generated files itself, which would lead to a circular task dependency.

The `maven:` scheme works similar to the `classpath:` scheme, but allows you to specify additional Maven coordinates
 to filter the dependency. The syntax is (without spaces)

```text
maven:<groupId>:<artifactId>:<extension>:<classifier>:<version>!path/to/resource
```

where all parts are optional, and trailing colons may be omitted.

Note that in contrast to the Maven JAXB2 Plugin, the dependency isn't resolved ad-hoc: it must still be
 listed in your Gradle build script.

You can think of the `maven:` scheme as an extension to `classpath:` with a filter for the JARs to be searched
 for resources. (In fact, `classpath:` is defined as an alias for `maven::!`.)
