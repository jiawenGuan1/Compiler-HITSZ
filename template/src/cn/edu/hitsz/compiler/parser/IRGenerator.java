package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成
public class IRGenerator implements ActionObserver {


    public SymbolTable table;
    private final Stack<Symbol> tokenStack = new Stack<>();
    private List<Instruction> IRList = new ArrayList<>();
    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        String number = "^[0-9]+$";
        Symbol curSymbol = new Symbol(currentToken);
        if(currentToken.getText().matches(number)){
            curSymbol.value = IRImmediate.of(Integer.parseInt(currentToken.getText()));
        }else{
            curSymbol.value = IRVariable.named(currentToken.getText());
        }
        tokenStack.push(curSymbol);
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        Symbol curToken_right, curToken_left;
        Symbol curNonTeiminal = new Symbol(production.head());
        IRVariable valueTemp;
        switch (production.index()) {
            case 6:     //S -> id = E;
                curToken_right = tokenStack.pop();
                tokenStack.pop();
                curToken_left = tokenStack.pop();
                valueTemp = (IRVariable) curToken_left.value;
                curNonTeiminal.value = null;
                IRList.add(Instruction.createMov(valueTemp, curToken_right.value));
                tokenStack.push(curNonTeiminal);
                break;
            case 7:     //S -> return E;
                curToken_right = tokenStack.pop();
                tokenStack.pop();
                curNonTeiminal.value = null;
                IRList.add(Instruction.createRet(curToken_right.value));
                tokenStack.push(curNonTeiminal);
                break;
            case 8:     //E -> E + A;
                curToken_right = tokenStack.pop();
                tokenStack.pop();
                curToken_left = tokenStack.pop();
                valueTemp = IRVariable.temp();  //生成临时变量
                IRList.add(Instruction.createAdd(valueTemp, curToken_left.value, curToken_right.value));
                curNonTeiminal.value = valueTemp;
                tokenStack.push(curNonTeiminal);
                break;
            case 9:     //E -> E - A;
                curToken_right = tokenStack.pop();
                tokenStack.pop();
                curToken_left = tokenStack.pop();
                valueTemp = IRVariable.temp();  //生成临时变量
                IRList.add(Instruction.createSub(valueTemp, curToken_left.value, curToken_right.value));
                curNonTeiminal.value = valueTemp;
                tokenStack.push(curNonTeiminal);
                break;
            case 10:    //E -> A;
            case 12:    //A -> B;
            case 14:    //B -> id;
                curNonTeiminal.value = tokenStack.pop().value;
                tokenStack.push(curNonTeiminal);
                break;
            case 11:    //A -> A * B;
                curToken_right = tokenStack.pop();
                tokenStack.pop();
                curToken_left = tokenStack.pop();
                valueTemp = IRVariable.temp();  //生成临时变量
                IRList.add(Instruction.createMul(valueTemp, curToken_left.value, curToken_right.value));
                curNonTeiminal.value = valueTemp;
                tokenStack.push(curNonTeiminal);
                break;
            case 13:    //B -> ( E );
                tokenStack.pop();
                curToken_right = tokenStack.pop();
                tokenStack.pop();
                curNonTeiminal.value = curToken_right.value;
                tokenStack.push(curNonTeiminal);
                break;
            case 15:    //B -> IntConst;
                curToken_right = tokenStack.pop();
                curNonTeiminal.value = curToken_right.value;
                tokenStack.push(curNonTeiminal);
                break;
            default:
                for(int i = 0; i < production.body().size(); i++) {
                    tokenStack.pop();
                }
                tokenStack.push(curNonTeiminal);
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // no action
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.table = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return IRList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

