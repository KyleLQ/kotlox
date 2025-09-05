package lox

/**
 * Simple printing class that shows the nesting structure of a syntax tree, in lisp like syntax
 */
class AstPrinter: Expr.Visitor<String>{

    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitAssignExpr(expr: Assign): String {
        TODO("Not yet implemented")
    }

    override fun visitBinaryExpr(expr: Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitCallExpr(expr: Call): String {
        TODO("Not yet implemented")
    }

    override fun visitGetExpr(expr: Get): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Grouping): String {
        return parenthesize("GROUPING", expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal): String {
        return if (expr.value != null) "${expr.value}" else "nil"
    }

    override fun visitLogicalExpr(expr: Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitSetExpr(expr: Set): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: This): String {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    override fun visitVariableExpr(expr: Variable): String {
        TODO("Not yet implemented")
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("($name")

        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }

        builder.append(")")
        return builder.toString()
    }
}

fun main() {
    val expression: Expr = Binary(
        Unary(
            Token(TokenType.MINUS, "-", null, 1),
            Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Grouping(
            Literal(45.67)
        )
    )

    println(AstPrinter().print(expression))
}