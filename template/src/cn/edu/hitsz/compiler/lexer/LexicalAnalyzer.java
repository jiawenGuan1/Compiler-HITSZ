package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * 词法分析器: 将输入文件的内容转化为 Token 列表
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    private String context;     // 保存文件内容
    private List<Token> tokens; // 保存分析得到的 Token 列表

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.tokens = new ArrayList<>();
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        StringBuffer stringBuffer = new StringBuffer();
        try(BufferedReader reader = new BufferedReader(new FileReader(path))){
            String line;
            while((line = reader.readLine()) != null){
                stringBuffer.append(line).append(System.lineSeparator());
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        this.context = stringBuffer.toString();
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        int pos = 0;
        while(pos < context.length()){
            char curChar = context.charAt(pos);

            if(Character.isWhitespace(curChar)){
                pos++;
            }else if(Character.isLetter(curChar)){
                // 处理标识符或关键字
                StringBuilder identifier = new StringBuilder();
                while(pos < context.length() && (Character.isLetterOrDigit(context.charAt(pos)) || context.charAt(pos) == '_')){
                    identifier.append(context.charAt(pos));
                    pos++;
                }
                String id = identifier.toString();
                // 判断是否是关键字或标识符
                if(TokenKind.isAllowed(id)){
                    tokens.add(Token.simple(TokenKind.fromString(id)));
                }else{
                    // 非关键字的标识符
                    TokenKind idKind = TokenKind.fromString("id");
                    tokens.add(Token.normal(idKind, id));
                    symbolTable.add(id);
                }
            }else if(Character.isDigit(curChar)){
                StringBuilder number = new StringBuilder();
                while(pos < context.length() && Character.isDigit(context.charAt(pos))){
                    number.append(context.charAt(pos));
                    pos++;
                }
                String numStr = number.toString();
                TokenKind intCostKind = TokenKind.fromString("IntConst");
                tokens.add(Token.normal(intCostKind, numStr));
            }else{
                // 处理其他符号
                switch (curChar) {
                    case '+':
                        tokens.add(Token.simple(TokenKind.fromString("+")));
                        pos++;
                        break;
                    case '-':
                        tokens.add(Token.simple(TokenKind.fromString("-")));
                        pos++;
                        break;
                    case '*':
                        tokens.add(Token.simple(TokenKind.fromString("*")));
                        pos++;
                        break;
                    case '/':
                        tokens.add(Token.simple(TokenKind.fromString("/")));
                        pos++;
                        break;
                    case '(':
                        tokens.add(Token.simple(TokenKind.fromString("(")));
                        pos++;
                        break;
                    case ')':
                        tokens.add(Token.simple(TokenKind.fromString(")")));
                        pos++;
                        break;
                    case '=':
                        tokens.add(Token.simple(TokenKind.fromString("=")));
                        pos++;
                        break;
                    case ',':
                        tokens.add(Token.simple(TokenKind.fromString(",")));
                        pos++;
                        break;
                    case ';':
                        tokens.add(Token.simple(TokenKind.fromString("Semicolon")));
                        pos++;
                        break;
                    default:
                        throw new RuntimeException("Unrecognized character: " + curChar);
                }
            }
        }
        tokens.add(Token.simple(TokenKind.eof()));
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
