package cap.manchou.codegen.generator

import cap.manchou.codegen.generator.error.InvalidExpressionException
import cap.manchou.codegen.generator.resolver.ExpressionResolver
import cap.manchou.codegen.strings.*
import java.io.File
import java.util.regex.Pattern

class CodeGenerator(private val template: String) {

  var templateFile: File? = null

  companion object {
    fun loadFromFile(templateFile: File) =
      CodeGenerator(templateFile.readText())
        .setTemplateFile(templateFile)

    fun wrapFunction(function: ((String) -> String)): (Any?) -> Any? {
      return { s -> function.invoke(s?.toString() ?: "") }
    }
  }

  private fun setTemplateFile(templateFile: File): CodeGenerator {
    this.templateFile = templateFile
    return this
  }

  private val expressionPattern = Pattern.compile("""\$\{[^}]*}""")
  private val forPattern = Pattern.compile("""${"\\$"}for\([^)]+\)\s*\{[^}]*}""")
  private val thenPattern = Pattern.compile("""\s*then\s*\{[^}]*}""")
  private val elsePattern = Pattern.compile("""\s*else\s*\{[^}]*}""")
  private val ifPattern =
    Pattern.compile("""\${'$'}if\{[^}]+\}(\s*then\s*\{[^}]*})?(\s*else\s*\{[^}]*})?""")

  private val functions: MutableMap<String, (Any?) -> Any?> = mutableMapOf(
    "upperCase" to wrapFunction(::upperCase),
    "label" to wrapFunction(::label),
    "lowerCase" to wrapFunction(::lowerCase),
    "capitalizedLabel" to wrapFunction(::capitalizedLabel),
    "camelCase" to wrapFunction(::camelCase),
    "kebabCase" to wrapFunction(::kebabCase),
    "pascalCase" to wrapFunction(::pascalCase),
    "snakeCase" to wrapFunction(::snakeCase),
    "dotCase" to wrapFunction(::dotCase),
    "titleCase" to wrapFunction(::titleCase),
    "isEmpty" to ::isEmpty,
  )

  fun registerFunction(name: String, function: (Any?) -> Any?) {
    functions[name] = function
  }

  fun generate(vararg args: Any): String {
    val params: HashMap<String, Any> = HashMap()

    args.forEachIndexed { index, arg ->
      params["arg$index"] = arg
      params["args[$index]"] = arg
    }

    return generate(params)
  }

  fun generate(params: Map<String, Any>): String {
    var output = template

    registerFunction("template") { s -> template(s, params) }

    val resolver = ExpressionResolver(functions, params)

    val matcherIf = ifPattern.matcher(template)
    while (matcherIf.find()) {
      val ifExpression = matcherIf.group()

      val parsedIfExpression = parseIfExpression(ifExpression)

      val value = parsedIfExpression.resolve(resolver)
      output = output.replace(ifExpression, value)
    }

    val matcherFor = forPattern.matcher(template)
    while (matcherFor.find()) {
      val forExpression = matcherFor.group()

      val parsedForExpression = parseForExpression(forExpression, resolver)

      var value = ""
      for (index in parsedForExpression.intProgression) {
        var forParams = params
        if (parsedForExpression.indexName != null) {
          forParams = params.plus(Pair(parsedForExpression.indexName, index))
        }
        val forFunctions = functions.plus("template" to { s -> template(s, forParams) })
        val forResolver = ExpressionResolver(forFunctions, forParams)

        value += forResolver.resolveExpression(parsedForExpression.toResolveExpression)
      }
      output = output.replace(forExpression, value)
    }

    val matcher = expressionPattern.matcher(template)
    while (matcher.find()) {
      val expression = matcher.group()
      val value = resolver.resolveExpression(extractExpression(expression))
      output = output.replace(expression, value)
    }

    return output
  }

  private fun parseIfExpression(ifExpression: String): ParsedIfExpression {
    val conditionExpression =
      ifExpression.substring(ifExpression.indexOf('{') + 1, ifExpression.indexOf('}')).trim()

    val elseMatcher = elsePattern.matcher(ifExpression)
    val elseFound = elseMatcher.find()
    val thenMatcher = if (elseFound) {
      thenPattern.matcher(ifExpression.substring(ifExpression.indexOf('}'), elseMatcher.start()))
    } else {
      thenPattern.matcher(ifExpression.substring(ifExpression.indexOf('}')))
    }

    return ParsedIfExpression(
      conditionExpression,
      if (thenMatcher.find()) extractExpression(thenMatcher.group()) else null,
      if (elseFound) extractExpression(elseMatcher.group()) else null
    )
  }

  private fun parseForExpression(
    forExpression: String,
    resolver: ExpressionResolver,
  ): ParsedForExpression {
    val loopExpression =
      forExpression.substring(forExpression.indexOf('(') + 1, forExpression.indexOf(')')).trim()
    var loop = loopExpression.split("step").map { it.trim() }
    var step: Any? = 1
    if (loop.size == 2) {
      step = resolver.resolveExpressionAsObject(loop[1].trim())
    }
    loop = loop[0].split("=").map { it.trim() }
    if (loop.size > 2) {
      throw InvalidExpressionException("Invalid for expression: $forExpression")
    }
    val indexName = if (loop.size == 2) loop[0].trim() else null
    val rangeExpression = if (loop.size == 2) loop[1].trim() else loop[0].trim()

    val (start, end) = rangeExpression.split("..")
      .map { resolver.resolveExpressionAsObject(it.trim()) }
    if (start == null || end == null) {
      throw InvalidExpressionException("Invalid for expression: $forExpression")
    }

    try {
      val iStart = toInt(start)
      val iEnd = toInt(end)
      val iStep = toInt(step)

      val intProgression =
        if (iStep < 0)
          iStart downTo iEnd step -iStep
        else
          iStart..iEnd step iStep

      return ParsedForExpression(
        indexName,
        intProgression,
        extractExpression(forExpression)
      )
    } catch (ex: NumberFormatException) {
      throw InvalidExpressionException("Invalid for expression: $forExpression", ex)
    }
  }

  private fun toInt(value: Any?): Int {
    return if (value is Number) value.toInt() else value?.toString()?.toInt() ?: 0
  }

  /** Return the expression between {...}. */
  private fun extractExpression(expression: String): String {
    return expression.substring(expression.indexOf('{') + 1, expression.lastIndexOf('}')).trim()
  }

  private fun isEmpty(o: Any?): Boolean {
    if (o == null) return true

    if (o is Array<*>) return o.isEmpty()

    if (o is Collection<*>) return o.isEmpty()

    if (o is Map<*, *>) return o.isEmpty()

    return o.toString().isEmpty()
  }

  private fun template(templatePath: Any?, params: Map<String, Any>): String {
    val file: File = fileFromPath(templatePath)

    if (!file.exists()) {
      throw InvalidExpressionException("Template file $templatePath not found")
    }
    return loadFromFile(file).generate(params)
  }

  private fun fileFromPath(path: Any?): File {
    var file: File? = null
    when (path) {
      is String -> {
        if (templateFile != null) {
          file = File(templateFile!!.parentFile, path)
        }
        if (file == null || !file.exists()) {
          file = File(path)
        }
        return file
      }

      is File -> return path
      else -> throw InvalidExpressionException("Invalid template path: $path")
    }
  }
}
