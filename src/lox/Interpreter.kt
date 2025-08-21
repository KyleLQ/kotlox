package lox

class Interpreter: Visitor<Any?>{
    override fun visitBinaryExpr(expr: Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        when(expr.operator.type) {
            TokenType.BANG_EQUAL -> return !isEqual(left, right)
            TokenType.EQUAL_EQUAL -> return isEqual(left, right)
            TokenType.GREATER -> return (left as Double) > (right as Double)
            TokenType.GREATER_EQUAL -> return (left as Double) >= (right as Double)
            TokenType.LESS -> return (left as Double) < (right as Double)
            TokenType.LESS_EQUAL -> return (left as Double) <= (right as Double)
            TokenType.MINUS -> return (left as Double) - (right as Double)
            TokenType.SLASH -> return (left as Double) / (right as Double)
            TokenType.STAR -> return (left as Double) * (right as Double)
            TokenType.PLUS -> {
                if (left is Double && right is Double) {
                    return left + right
                }

                if (left is String && right is String) {
                    return left + right
                }
            }
            else -> return null // unreachable
        }

        return null // unreachable
    }

    override fun visitGroupingExpr(expr: Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> -(right as Double)
            TokenType.BANG -> !isTruthy(right)
            else -> null // unreachable
        }
    }

    private fun isTruthy(obj: Any?): Boolean {
        if ( obj == null) {
            return false
        }
        if (obj is Boolean) {
            return obj
        }
        return true
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) return true
        if (a == null) return false

        return a == b
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }
}