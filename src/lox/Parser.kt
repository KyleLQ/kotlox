package lox

/**
 * Recursive descent parser
 */
class Parser(private val tokens: List<Token>) {
    private class ParseError: RuntimeException()

    private var current = 0

    // returns every statement until it hits the EOF token.
    fun parse(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()
        while (!isAtEnd()) {
            statements.add(declaration())
        }

        return statements
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Variable) {
                val name = expr.name
                return Assign(name, value)
            } else if (expr is Get) {
                val get = expr
                return Set(get.obj, get.name, value)
            }

            error(equals, "Invalid assignment target")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Logical(expr, operator, right)
        }

        return expr
    }

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.CLASS)) return classDeclaration()
            if (match(TokenType.FUN)) return function("function")
            if (match(TokenType.VAR)) return varDeclaration()

            return statement()
        } catch(error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect Class name.")
        consume(TokenType.LEFT_BRACE, "Expect \'{\' before class body.")

        val methods = mutableListOf<Function>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(TokenType.RIGHT_BRACE, "Expect \'}\' after class body.")

        return Class(name, methods)
    }

    private fun statement(): Stmt {
        if (match(TokenType.FOR)) return forStatement()
        if (match(TokenType.IF)) return ifStatement()
        if (match(TokenType.PRINT)) return printStatement()
        if (match(TokenType.RETURN)) return returnStatement()
        if (match(TokenType.WHILE)) return whileStatement()
        if (match(TokenType.LEFT_BRACE)) return Block(block())

        return expressionStatement()
    }

    // this "desugars" the for loop into while loop AST nodes
    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect \'(\' after \'for\'.")

        val initializer = if (match(TokenType.SEMICOLON)) {
            null
        } else if (match(TokenType.VAR)) {
            varDeclaration()
        } else {
            expressionStatement()
        }

        var condition = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            null
        }
        consume(TokenType.SEMICOLON, "Expect \';\' after loop condition.")

        val increment = if (!check(TokenType.RIGHT_PAREN)) {
            expression()
        } else {
            null
        }
        consume(TokenType.RIGHT_PAREN, "Expect \')\' after for clauses.")
        var body = statement()

        if (increment != null) {
           body = Block(listOf(body, Expression(increment)))
        }

        if (condition == null) {
            condition = Literal(true)
        }
        body = While(condition, body)

        if (initializer != null) {
            body = Block(listOf(initializer, body))
        }

        return body
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect \'(\' after \'if\'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect \')\' after if condition.")

        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) {
            elseBranch = statement()
        }

        return If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect \';\' after value.")
        return Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!check(TokenType.SEMICOLON)) {
            value = expression()
        }

        consume(TokenType.SEMICOLON, "Expect \';\' after return value.")
        return Return(keyword, value)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) {
            initializer = expression()
        }

        consume(TokenType.SEMICOLON, "Expect \';\' after variable declaration.")

        return Var(name, initializer)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect \'(\' after \'while\'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect \')\' after condition.")
        val body = statement()

        return While(condition, body)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect \';\' after expression.")
        return Expression(expr)
    }

    private fun function(kind: String): Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect \'(\' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect \')\' after parameters.")

        consume(TokenType.LEFT_BRACE, "Expect \'{\' before $kind body.")
        val body = block()
        return Function(name, parameters, body)
    }

    private fun block(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()

        // check isAtEnd to prevent infinite loop in case right brace is forgotten
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration())
        }

        consume(TokenType.RIGHT_BRACE, "Expect \'}\' after block.")
        return statements
    }

    // is left associative
    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // is left associative
    private fun comparison(): Expr {
        var expr = term()

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // is left associative
    private fun term(): Expr {
        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // is left associative
    private fun factor(): Expr {
        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Unary(operator, right)
        }

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expect property name after \'.\'.")
                expr = Get(expr,name)
            } else {
                break
            }
        }

        return expr
    }

    // helper function for call() that parses the function's
    // argument list.
    // Max limit = 255 arguments for a function.
    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.") // reports error, but doesn't enter panic mode.
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect \')\' after arguments.")

        return Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        if (match(TokenType.FALSE)) return Literal(false)
        if (match(TokenType.TRUE)) return Literal(true)
        if (match(TokenType.NIL)) return Literal(null)

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Literal(previous().literal)
        }

        if (match(TokenType.THIS)) {
            return This(previous())
        }

        if (match(TokenType.IDENTIFIER)) {
            return Variable(previous())
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect \')\' after expression.")
            return Grouping(expr)
        }

        throw error(peek(), "Expect expression.")
    }

    // if current token matches any of types, advance current pointer and return true
    // otherwise, do nothing to current and return false
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    // advances if current token type matches type. Else throws error
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    // check if current token type matches type
    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    // advance current by one, and return previous token that was pointed to by current
    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun error(token: Token, message: String): ParseError {
        lox.error(token, message)
        return ParseError()
    }

    // provides panic mode synchronization. Discards tokens until it sees either
    // a statement end or beginning. (doesn't perfectly handle all cases)
    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return

            when(peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.PRINT,
                TokenType.RETURN -> return
                else -> advance()
            }
        }
    }
}