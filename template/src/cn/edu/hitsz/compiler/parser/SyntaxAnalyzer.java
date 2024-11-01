package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.*;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

//TODO: 实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();
    private List<Token> tokens;     // 存储词法单元的列表
    private LRTable lrTable;        // 存储 LR 分析表


    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // TODO: 加载词法单元
        // 将 tokens 全部加载到一个 List 中，后续操作可以依次处理这些 tokens
        this.tokens = new ArrayList<>();
        tokens.forEach(this.tokens::add);
    }

    public void loadLRTable(LRTable table) {
        // TODO: 加载 LR 分析表
        // 存储传入的 LRTable
        this.lrTable = table;
    }

    public void run() {
        // TODO: 实现驱动程序
        Stack<Status> statusStack = new Stack<>();  // 建立状态栈并初始化
        statusStack.push(lrTable.getInit());

        // 初始化符号栈，使用自定义的 Symbol 类来存储 Token 和 NonTerminal
        Stack<Symbol> symbolStack = new Stack<>();
        symbolStack.push(new Symbol(Token.eof()));

        // 获取 token 流的迭代器
        Iterator<Token> tokenIterator = tokens.iterator();
        Token currentToken = tokenIterator.hasNext() ? tokenIterator.next() : null;

        while(true){
            Status currentStatus = statusStack.peek();  // peek()返回栈顶的元素，但不会移除该元素
            Action action = lrTable.getAction(currentStatus, currentToken);

            if(action.getKind() == Action.ActionKind.Shift){
                // Shift操作 -- 把Action的状态压入状态栈，对应的token压入符号栈
                callWhenInShift(currentStatus, currentToken);   // 通知观察者
                Status newStatus = action.getStatus();  // 获取 shift 后的新状态
                statusStack.push(newStatus);    // 将新状态压入状态栈
                symbolStack.push(new Symbol(currentToken));  // 将当前词法单元包装为 Symbol 压入符号栈

                // 移动到下一个token
                if(tokenIterator.hasNext()){
                    currentToken = tokenIterator.next();    // 更新 currentToken
                }else{
                    currentToken = null;
                }
            }else if(action.getKind() == Action.ActionKind.Reduce){
                // Reduce操作 -- 根据产生式长度，符号栈和状态栈均弹出对应长度个token和状态；
                // 产生式左侧的非终结符压入符号栈，根据符号栈和状态栈栈顶状态获取Goto表的状态，压入状态栈
                Production production = action.getProduction(); // 获取待规约的产生式
                callWhenInReduce(currentStatus, production);    // 通知观察者

                // 弹出符号栈和状态栈，弹出的数量与产生式右部符号数相同
                for(int i = 0; i < production.body().size(); i++){
                    statusStack.pop();
                    symbolStack.pop();
                }

                // 规约后获取 goto 表中的新状态
                Status topStatus = statusStack.peek();  // 获取当前状态栈顶
                NonTerminal head = production.head();   // 获取产生式的左部（非终结符）
                Status gotoStatus = lrTable.getGoto(topStatus, head);
                statusStack.push(gotoStatus);   // 将新状态压入状态栈
                // 符号栈也需要把产生式左部的非终结符压入
                symbolStack.push(new Symbol(head)); // 将 NonTerminal 压入符号栈
            }else if(action.getKind() == Action.ActionKind.Accept){
                // Accept 操作 -- 语法分析执行结束
                callWhenInAccept(currentStatus);    // 通知观察者
                break;  // 语法分析成功，退出循环
            }else{
                // 错误处理
                throw new RuntimeException("Unexpected action: " + action.getKind());
            }
        }
    }
}
