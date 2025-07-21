package cap.manchou.codegen

import cap.manchou.codegen.error.InvalidExpressionException
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class ExpressionResolver(
  private val functions: Map<String, ((Any?) -> Any?)>,
  private val variables: Map<String, Any>,
) {

  private val nullValue = "null";

  private val integerPattern = Pattern.compile("""-?\d+""")
  private val numberPattern = Pattern.compile("""-?\d+\.\d+""")
  private val booleanMatcher = Pattern.compile("""true|false""")
  private val dotAccessPattern = Pattern.compile("""[a-zA-Z][\w\[\]]*\.[a-zA-Z][\w\[\].]*""")
  private val arrayAccessPattern = Pattern.compile("""[a-zA-Z][\w]*\[[^\[]*]""")
  private val literalPattern = Pattern.compile(""""[^"]*"""")
  private val functionPattern = Pattern.compile("""[a-zA-Z]\w*\(.*\)""")
  private val comparisonPattern = Pattern.compile(""".+(<|>|<=|>=|==|!=).+""")

  /**
   * Resolvers ordered by priority.
   */
  private val resolvers = linkedMapOf<Pattern, (String) -> Any?>(
    integerPattern to String::toLong,
    numberPattern to String::toDouble,
    booleanMatcher to String::toBoolean,
    dotAccessPattern to this::resolveDotAccess,
    arrayAccessPattern to this::resolveArrayAccess,
    literalPattern to this::resolveLiteral,
    functionPattern to this::resolveFunction,
    comparisonPattern to this::resolveComparison
  )

  fun resolveExpression(expression: String): String {
    return resolveExpressionAsObject(expression)?.toString() ?: nullValue
  }

  fun resolveExpressionAsObject(expression: String): Any? {
    if (expression.isEmpty()) {
      return ""
    }

    if (expression == nullValue) {
      return null
    }

    if (variables.containsKey(expression)) {
      return variables[expression]
    }

    resolvers.forEach { (pattern, resolver) ->
      val matcher = pattern.matcher(expression)
      if (matcher.matches()) {
        return resolver.invoke(matcher.group())
      }
    }

    throw InvalidExpressionException("Unresolved expression $expression")
  }

  private fun resolveDotAccess(expression: String): Any? {
    val dotIndex = expression.lastIndexOf('.')
    val objectName = expression.substring(0, dotIndex).trim()
    val fieldName = expression.substring(dotIndex + 1, expression.length).trim()

    val objectValue = resolveExpressionAsObject(objectName)
      ?: throw InvalidExpressionException("Expression $objectName is null. Could not resolve field $fieldName")

    try {
      return resolveFieldValue(objectValue, fieldName)
    } catch (ex: InvalidExpressionException) {
      throw InvalidExpressionException("Unknown property $fieldName in variable $objectName", ex)
    }
  }

  private fun resolveArrayAccess(expression: String): Any? {
    val bracketIndex = expression.indexOf('[')
    val objectName = expression.substring(0, bracketIndex).trim()
    val fieldName = expression.substring(bracketIndex + 1, expression.length - 1).trim()

    val objectValue = resolveExpressionAsObject(objectName)
      ?: throw InvalidExpressionException("Unknown variable $objectName")

    try {
      return resolveArrayValue(objectValue, resolveExpressionAsObject(fieldName))
    } catch (ex: InvalidExpressionException) {
      throw InvalidExpressionException("Unknown property $fieldName in variable $objectName", ex)
    }
  }

  private fun resolveArrayValue(objectValue: Any, fieldName: Any?): Any? {
    if (objectValue is List<*> && fieldName is Number) {
      return objectValue[fieldName.toInt() - 1]
    }
    if (objectValue is Array<*> && fieldName is Number) {
      return objectValue[fieldName.toInt() - 1]
    }
    if (fieldName is String) {
      return resolveFieldValue(objectValue, fieldName)
    }
    throw InvalidExpressionException("Unknown property $fieldName")
  }

  private fun resolveFieldValue(objectValue: Any, fieldName: String): Any? {

    val dotIndex = fieldName.indexOf('.')
    if (dotIndex > 0) {
      throw InvalidExpressionException("Unknown property $fieldName")
    }

    if (objectValue is Map<*, *>) {
      return objectValue[fieldName]
        ?: throw InvalidExpressionException("Unknown property $fieldName")
    }
    if (objectValue is Collection<*> && (fieldName == "size" || fieldName == "length")) {
      return objectValue.size
    }
    if (objectValue is Array<*> && (fieldName == "size" || fieldName == "length")) {
      return objectValue.size
    }

    try {
      val clazz = objectValue::class as KClass<in Any>

      return clazz.memberProperties
        .first { it.name == fieldName }
        .also { it.isAccessible = true }
        .getter(objectValue)

    } catch (ex: NoSuchElementException) {
      throw InvalidExpressionException("Unknown property $fieldName")
    }

  }

  private fun resolveLiteral(literalExpression: String): String {
    return literalExpression.substring(1, literalExpression.length - 1)
  }

  private fun resolveFunction(functionExpr: String): Any? {
    val parenthesisIndex = functionExpr.indexOf('(')
    val functionName = functionExpr.substring(0, parenthesisIndex)
    val functionArgs = functionExpr.substring(parenthesisIndex + 1, functionExpr.length - 1)
    val function = functions[functionName]
      ?: throw InvalidExpressionException("Unknown function $functionName() in expression $functionExpr")

    try {
      return function.invoke(resolveExpressionAsObject(functionArgs.trim()))
    } catch (ex: InvalidExpressionException) {
      throw InvalidExpressionException("Unresolved expression $functionExpr", ex)
    }
  }

  private fun resolveComparison(comparisonExpr: String): Boolean {
    val comparator = when {
      comparisonExpr.contains(Comparator.LTE.expression) -> Comparator.LTE
      comparisonExpr.contains(Comparator.GTE.expression) -> Comparator.GTE
      comparisonExpr.contains(Comparator.LT.expression) -> Comparator.LT
      comparisonExpr.contains(Comparator.GT.expression) -> Comparator.GT
      comparisonExpr.contains(Comparator.EQ.expression) -> Comparator.EQ
      comparisonExpr.contains(Comparator.NEQ.expression) -> Comparator.NEQ
      else -> throw InvalidExpressionException("Unknown comparator in expression $comparisonExpr")
    }
    val comparatorIndex = comparisonExpr.indexOf(comparator.expression)
    val leftExpr = comparisonExpr.substring(0, comparatorIndex).trim()
    val rightExpr = comparisonExpr.substring(
      comparatorIndex + comparator.expression.length,
      comparisonExpr.length
    ).trim()

    val left = resolveExpressionAsObject(leftExpr)
    val right = resolveExpressionAsObject(rightExpr)
    try {
      return comparator.resolve(left, right)
    } catch (ex: InvalidExpressionException) {
      throw InvalidExpressionException("Unresolved comparison expression $comparisonExpr", ex)
    }
  }

  private enum class Comparator(val expression: String) {
    LTE("<="),
    GTE(">="),
    LT("<"),
    GT(">"),
    EQ("=="),
    NEQ("!=");

    fun resolve(left: Any?, right: Any?): Boolean {

      if (left is Number && right is Number) {
        return compare(left.toDouble(), right.toDouble())
      }
      if (left is String && right is String) {
        return compare(left, right)
      }

      if (this == EQ) {
        return left == right
      }
      if (this == NEQ) {
        return left != right
      }
      throw InvalidExpressionException("Unsupported comparison $expression")
    }

    fun <U> compare(left: Comparable<U>, right: U): Boolean {
      return when (this) {
        LTE -> left <= right
        GTE -> left >= right
        LT -> left < right
        GT -> left > right
        EQ -> left == right
        NEQ -> left != right
      }
    }
  }
}