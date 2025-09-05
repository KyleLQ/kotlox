package lox

import java.util.*

class Resolver(private val interpreter: Interpreter): Expr.Visitor<Unit>, Stmt.Visitor<Unit>{
    private enum class FunctionType {
        NONE,
        FUNCTION
    }

    // tracks local block scopes only. Global scope variables at top level are not tracked here.
    // The Boolean represents whether the variable initializer has been resolved.
    private val scopes = Stack<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE

    override fun visitBlockStmt(stmt: Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitExpressionStmt(stmt: Expression) {
        resolve(stmt.expression)
    }

    // define the name eagerly to allow function to recursively refer to itself
    override fun visitFunctionStmt(stmt: Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) resolve(stmt.elseBranch)
    }

    override fun visitPrintStmt(stmt: Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Return) {
        if (currentFunction == FunctionType.NONE) {
            lox.error(stmt.keyword, "Can't return from top-level code.")
        }

        if (stmt.value != null) {
            resolve(stmt.value)
        }
    }

    override fun visitVarStmt(stmt: Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitAssignExpr(expr: Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Call) {
        resolve(expr.callee)

        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitGroupingExpr(expr: Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal) {
    }

    override fun visitLogicalExpr(expr: Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnaryExpr(expr: Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Variable) {
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            lox.error(expr.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    fun resolve(statements: List<Stmt?>) {
        for (statement in statements) {
            resolve(statement)
        }
    }

    private fun resolve(stmt: Stmt?) {
        stmt?.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolveFunction(function: Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

    // adds the variable to the innermost scope, so it shadows any outer one,
    // but has not resolved its initializer yet
    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            lox.error(name, "Already a variable with this name in this scope.")
        }

        scope[name.lexeme] = false
    }

    // variable has resolved its initializer
    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek()[name.lexeme] = true
    }

    // starts from innermost to outermost scope. Passes in the
    // difference between the current innermost scope and
    // the scope where the variable was found.
    // If don't find, leave it unresolved and assume it's global.
    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.size - 1 downTo 0 step 1) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }
}