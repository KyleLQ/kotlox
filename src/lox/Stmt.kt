package lox


abstract class Stmt {
    interface Visitor<R> {
        fun visitExpressionStmt(stmt: Expression): R
        fun visitPrintStmt(stmt: Print): R
    }
    abstract fun <R> accept(visitor: Visitor<R>): R
}

class Expression(val expression: Expr): Stmt() {
    override fun <R> accept(visitor: Visitor<R>): R {
        return visitor.visitExpressionStmt(this)
    }
}

class Print(val expression: Expr): Stmt() {
    override fun <R> accept(visitor: Visitor<R>): R {
        return visitor.visitPrintStmt(this)
    }
}

