# Gradle XJC Plugin

Invokes the `xjc` binding compiler from a Gradle build.


## Usage

### Applying the plugin

To use the XJC plugin, include either of the following in your build script:

#### New Plugins DSL (Gradle 2.1+)

```groovy
plugins {
    id 'org.unbroken-dome.xjc' version '0.1.1'
}
```

#### Traditional (Gradle 1.x/2.0)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'org.unbroken-dome.gradle-plugins:gradle-xjc-plugin:0.1.1'
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
 
### Common Parameters
 
| Parameter | Type | Command-line equivalent | Default / convention value |
|---|---|---|---|
| `source` | `FileTree` | | all `*.xsd` files under `src/main/schema` |
| `bindingFiles` | `FileCollection` | `-b` | all `*.xjb` files under `src/main/schema` |
| `outputDirectory` | `File` | `-d` | `build/xjc/generated-sources` |
| `extension` | `boolean` | `-extension` | `false` |

### Additional Parameters

The following parameters are used less commonly, to fine-tune the generation process or
 in special scenarios. Please refer to the
 [`xjc` command line tool](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/xjc.html)
 documentation for their exact meaning.

| Parameter | Type | Command-line equivalent | Default / convention value |
|---|---|---|---|
| `catalogs` | `FileCollection` | `-catalog` | not set |
| `classpath` | `FileCollection` | `-classpath` | all modules in the `xjcClasspath` configuration |
| `episodes` | `File` | | all modules in the `xjcEpisode` configuration |
| `episodeTargetFile` | `File` | `-episode` | `build/xjc/sun-jaxb.episode` |
| `packageLevelAnnotations` | `boolean` | `-npa` (`true` if not present) | `true` |
| `quiet` | `boolean` | `-quiet` | `false` |
| `readOnly` | `boolean` | `-readOnly` | `false` |
| `strictCheck` | `boolean` | `-nv` (`true` if not present) | `true` | 
| `targetPackage` | `String` | `-p` | not set |
| `targetVersion` | `String` | `-target` | use latest version  |
| `verbose` | `boolean` | `-verbose` | `false` |


Additionally, the following parameter may be used to control the output code generation:

| Parameter | Type | Description | Default value |
|---|---|---|---|
| `encoding` | `String` | Encoding for generated Java files | `UTF-8` |


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

The plugin will automatically use the `extension` mode if there are parameters starting with `-X`
 present.


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
 
Of course, you may use a different configuration, like `compile`, as the source of episode JARs:

```groovy
xjcGenerate {
    episodes compile
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
