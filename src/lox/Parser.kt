package lox

/**
 * Recursive descent parser
 */
class Parser(private val tokens: List<Token>) {
    private class ParseError: RuntimeException()

    private var current = 0

    // returns a syntax tree, or null if there is a parse error
    fun parse(): Expr? {
        return try {
            ternaryOperator()
        } catch(error: ParseError) {
            null
        }
    }

    /**
     Peek? doesn't make sense. Don't know how long expression will be ahead of time.
     */
    private fun ternaryOperator(): Expr {
        var expr = commaOperator()

        if (match(TokenType.QUESTION)) {
            val leftOperator = previous()
            val mid = commaOperator()
            val rightOperator = consume(TokenType.COLON, "Incomplete ternary expression.")
            val right = ternaryOperator()
            expr = Ternary(expr, leftOperator, mid, rightOperator, right)
        }

        return expr
    }

    private fun commaOperator(): Expr {
        var expr = expression()
        while (match(TokenType.COMMA)) {
            expr = expression()
        }

        return expr
    }

    private fun expression(): Expr {
        return equality()
    }

    // is left associative
    private fun equality(): Expr {
        if (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val previous = previous()
            comparison()
            throw error(previous, "Expecting a left operand for ${previous.lexeme}")
        }

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
        if (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val previous = previous()
            term()
            throw error(previous, "Expecting a left operand for ${previous.lexeme}")
        }

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
        if (match(TokenType.PLUS)) {
            val previous = previous()
            factor()
            throw error(previous, "Expecting a left operand for ${previous.lexeme}")
        }

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
        if (match(TokenType.SLASH, TokenType.STAR)) {
            val previous = previous()
            unary()
            throw error(previous, "Expecting a left operand for ${previous.lexeme}")
        }

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

        return primary()
    }

    private fun primary(): Expr {
        if (match(TokenType.FALSE)) return Literal(false)
        if (match(TokenType.TRUE)) return Literal(true)
        if (match(TokenType.NIL)) return Literal(null)

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Literal(previous().literal)
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = ternaryOperator()
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