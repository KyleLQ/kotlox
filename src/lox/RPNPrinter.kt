package lox

import java.util.Stack

class RPNPrinter : Visitor<Unit> {
    // stack not necessary, just use call stack
    private val stack = Stack<String>()

    fun printRPN(expr: Expr): String {
        expr.accept(this)
        val stringBuilder = StringBuilder()
        while (!stack.isEmpty()) {
            stringBuilder.append(stack.pop())
        }
        return stringBuilder.toString()
    }

    override fun visitBinaryExpr(expr: Binary) {
        stack.push(expr.operator.lexeme + " ")
        expr.left.accept(this)
        expr.right.accept(this)
    }

    override fun visitGroupingExpr(expr: Grouping) {
        stack.push("GROUPING ")
        expr.expression.accept(this)
    }

    override fun visitLiteralExpr(expr: Literal) {
        stack.push((if (expr.value != null) "${expr.value}" else "nil") + " ")
    }

    override fun visitUnaryExpr(expr: Unary) {
        // use different symbol for unary minus
        val lexeme = if (expr.operator.type == TokenType.MINUS) "NEG" else expr.operator.lexeme
        stack.push("$lexeme ")
        expr.right.accept(this)
    }
}

fun main() {
    val expression: Expr = Binary(
        Grouping(
            Binary(
                Literal(1),
                Token(TokenType.PLUS, "+", null, 1),
                Unary(
                    Token(TokenType.MINUS, "-", null, 1),
                    Literal(2)
                )
            )
        ),
        Token(TokenType.STAR, "*", null, 1),
        Grouping(
            Binary(
                Literal(4),
                Token(TokenType.MINUS, "-", null, 1),
                Literal(3)
            )
        )
    )

    val expression2: Expr = Binary(
        Binary(
            Literal(1),
            Token(TokenType.PLUS, "+", null, 1),
            Literal(2)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Binary(
            Literal(4),
            Token(TokenType.MINUS, "-", null, 1),
            Literal(3)
        )
    )

    println(AstPrinter().print(expression))
    println(RPNPrinter().printRPN(expression))
}