import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

val cSharpToKotlinTypes = mapOf<String, String>(
    "uint" to "Int",
    "int" to "Int",
    "bool" to "Boolean",
    "float" to "Float",
    "string" to "String",
    "IList" to "List",
    "IEnumerable" to "List",
    "byte" to "Byte",
    "DateTime" to "String",
    "ushort" to "Short"
)

fun Map<String, String>.containsStartOfValue(value: String): Boolean {
    for (entry in this) {
        if (value.startsWith(entry.value)) return true
    }
    return false
}

val CSharpParser.Class_bodyContext.properties: Map<String, String>
    get() {
        val properties = mutableMapOf<String, String>()
        class_member_declarations()?.class_member_declaration()?.forEach {
            it.common_member_declaration().typed_member_declaration()?.apply {
                val property = this.property_declaration() ?: return@apply
                var type = this.type_().text
                cSharpToKotlinTypes.forEach { key, string ->
                    type = type.replace(key, string)
                }
                if (type.contains("[]")) {
                    type = type.replace("[]", "")
                    type = "List<$type>"
                }
                properties[property.member_name().text] = type
            }
        }
        return properties
    }

fun CSharpParser.Class_bodyContext.validProperties(classpath: List<CSharpParser.Class_definitionContext>): Map<String, String> {
    val properties = mutableMapOf<String, String>()
    this.properties.forEach { key, value ->
        if (cSharpToKotlinTypes.containsStartOfValue(value) || classpath.firstOrNull { value.replace("?", "").replace("List<", "").replace(">","") == (it.name) } != null) {
            properties[key] = value
        }
    }
    return properties
}

val CSharpParser.Namespace_member_declarationContext.asClass: CSharpParser.Class_definitionContext?
    get() = type_declaration()?.class_definition()

val CSharpParser.Class_definitionContext.name: String
    get() = this.identifier().text

val CSharpParser.Class_definitionContext.properties: Map<String, String>
    get() = this.class_body().properties

fun String.asCompilationUnit(): CSharpParser.Compilation_unitContext {
    val lexer = CSharpLexer(CharStreams.fromString(this))
    val parser = CSharpParser(CommonTokenStream(lexer))
    return parser.compilation_unit()
}

fun File.asCompilationUnit(): CSharpParser.Compilation_unitContext = this.readText().asCompilationUnit()

val CSharpParser.Compilation_unitContext.classes: List<CSharpParser.Class_definitionContext>
    get() {
        val list = mutableListOf<CSharpParser.Class_definitionContext>()
        this.namespace_member_declarations().namespace_member_declaration().forEach {
            it.namespace_declaration().namespace_body().namespace_member_declarations().namespace_member_declaration().forEach {
                it.asClass?.let {
                    list.add(it)
                }
            }
        }
        return list
    }

fun generateKotlinSrc(classes: List<CSharpParser.Class_definitionContext>): String = buildString {
    append("import kotlinx.serialization.Serializable\n")
    classes.forEach {
        val properties = it.class_body().validProperties(classes).map { "val ${it.key}: ${it.value}" }
        if (properties.isNotEmpty()){
            append("@Serializable ")
            append("data class ${it.name}(${properties.joinToString()}) \n")
        }
    }
}

fun generateKotlinDirSrc(directories: List<File>): String {
    val classes = mutableListOf<CSharpParser.Class_definitionContext>()
    directories.forEach {
        it.walkTopDown().forEach {
            if (!it.isDirectory) classes.addAll(it.asCompilationUnit().classes)
        }
    }
    return generateKotlinSrc(classes)
}

fun main() {
    val lexer = CSharpLexer(CharStreams.fromPath(File("test.cs").toPath()))
    val parser = CSharpParser(CommonTokenStream(lexer))
    val compilationUnit = parser.compilation_unit()
    //println(generateKotlinSrc(compilationUnit.classes))
    println(generateKotlinDirSrc(listOf(File("Models"))))
}