package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

public class Symbol {
    // 包装类，用于存储 Token 或 NonTerminal
    public Token token;
    public NonTerminal nonTerminal;
    SourceCodeType type = null;
    IRValue value = null;
    public Symbol(Token token) {
        this.token = token;
        this.nonTerminal = null;
    }
    public Symbol(NonTerminal nonTerminal) {
        this.token = null;
        this.nonTerminal = nonTerminal;
    }
    public boolean isToken() {
        return token != null;
    }
    public boolean isNonTerminal() {
        return nonTerminal != null;
    }
    public Token getToken() {
        return token;
    }
    public NonTerminal getNonTerminal() {
        return nonTerminal;
    }
}
