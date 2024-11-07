package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.lang.classfile.instruction.SwitchCase;
import java.util.*;
import java.util.stream.Collectors;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */

    enum Register {
        t0, t1, t2, t3, t4, t5, t6
    }

    private final List<Instruction> instructions = new ArrayList<>();

    BMap<IRValue, Register> registerBMap = new BMap<>();

    private final List<String> asmInstructions = new ArrayList<>(List.of(".text"));

    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        for(Instruction instr: originInstructions) {
            InstructionKind instrKind = instr.getKind();
            // 判断是否为ret指令，如果是ret指令，则直接丢弃后续指令
            if(instrKind.isReturn()) {
                instructions.add(instr);
                break;
            }
            // 如果指令是一元的 (有返回值, 有一个参数), 直接存入instructis即可
            if(instrKind.isUnary()) {
                instructions.add(instr);
            } else if(instrKind.isBinary()) {   // 如果指令是二元的 (有返回值, 有两个参数)，分类讨论
                IRValue lhs = instr.getLHS();
                IRValue rhs = instr.getRHS();
                IRVariable result = instr.getResult();
                // 情况一：如果两个操作数均为立即数，则将两个立即数直接进行BinaryOp操作求得结果，然后替换为MOV指令
                if(lhs.isImmediate() && rhs.isImmediate()) {
                    int opResult = 0;
                    if(instrKind == InstructionKind.ADD) {
                        opResult = ((IRImmediate)lhs).getValue() + ((IRImmediate)rhs).getValue();
                    }else if(instrKind == InstructionKind.SUB) {
                        opResult = ((IRImmediate)lhs).getValue() - ((IRImmediate)rhs).getValue();
                    }else if(instrKind == InstructionKind.MUL) {
                        opResult = ((IRImmediate)lhs).getValue() * ((IRImmediate)rhs).getValue();
                    }else{
                        System.out.println("Error");
                    }
                    instructions.add(Instruction.createMov(result, IRImmediate.of(opResult)));
                // 情况二：如果左操作数为立即数，右操作数为变量，且运算类型为减法或乘法，
                // 则用MOV指令将立即数存入临时变量temp，转为无立即数指令
                // 如果为加法，则只需要将立即数移至右边即可
                }else if(lhs.isImmediate() && rhs.isIRVariable()){
                    if(instrKind == InstructionKind.ADD){
                        instructions.add(Instruction.createAdd(result, rhs, lhs));
                    }else if(instrKind == InstructionKind.SUB){
                        IRVariable temp = IRVariable.temp();
                        instructions.add(Instruction.createMov(temp, lhs));
                        instructions.add(Instruction.createSub(result, temp, rhs));
                    }else if(instrKind == InstructionKind.MUL){
                        IRVariable temp = IRVariable.temp();
                        instructions.add(Instruction.createMov(temp, lhs));
                        instructions.add(Instruction.createMul(result, temp, rhs));
                    }else{
                        System.out.println("Error");
                    }
                // 情况三：如果左操作数为变量，右操作数为立即数，且运算类型为减法或乘法，
                // 则用MOV指令将立即数存入临时变量temp，转为无立即数指令
                // 如果为加法，不需要额外操作，直接加入instructions即可
                }else if(lhs.isIRVariable() && rhs.isImmediate()){
                    if(instrKind == InstructionKind.ADD){
                        instructions.add(instr);
                    }else if(instrKind == InstructionKind.SUB){
                        IRVariable temp = IRVariable.temp();
                        instructions.add(Instruction.createMov(temp, rhs));
                        instructions.add(Instruction.createSub(result, lhs, temp));
                    }else if(instrKind == InstructionKind.MUL){
                        IRVariable temp = IRVariable.temp();
                        instructions.add(Instruction.createMov(temp, rhs));
                        instructions.add(Instruction.createMul(result, lhs, temp));
                    }else{
                        System.out.println("Error");
                    }
                // 情况四：如果两个操作数均为变量，直接加入instructions即可
                }else{
                    instructions.add(instr);
                }
            }
        }
    }

    public void VariableToRegister(IRValue operands, int index){
        // 立即数无需分配寄存器
        if(operands.isImmediate()){
            return;
        }
        // 当前变量已经存在寄存器中，则无需再分配寄存器
        if(registerBMap.containsKey(operands)){
            return;
        }
        // 寻找空闲的寄存器
        for(Register register: Register.values()){
            if(!registerBMap.containsValue(register)){
                registerBMap.replace(operands, register);
                return;
            }
        }
        // 若无空闲寄存器，则夺取不再使用的变量所占的寄存器
        Set<Register> notUseRegs = new HashSet<>(List.of(Register.values()));
        for(int i = index; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            // 遍历搜索不再使用的变量
            for(IRValue irValue: instr.getOperands()){
                notUseRegs.remove(registerBMap.getByKey(irValue));
            }
        }
        if(!notUseRegs.isEmpty()){
            registerBMap.replace(operands, notUseRegs.iterator().next());
            return;
        }
        // 否则无法分配寄存器并报错
        throw new RuntimeException("No enough registers!");
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        int i = 0;
        String asmCode = null;
        for(Instruction instr: instructions){
            InstructionKind instrKind = instr.getKind();
            switch (instrKind){
                case ADD -> {
                    IRValue lhs = instr.getLHS();
                    IRValue rhs = instr.getRHS();
                    IRVariable result = instr.getResult();
                    VariableToRegister(lhs, i);
                    VariableToRegister(rhs, i);
                    VariableToRegister(result, i);
                    Register lhsReg = registerBMap.getByKey(lhs);
                    Register rhsReg = registerBMap.getByKey(rhs);
                    Register resultReg = registerBMap.getByKey(result);
                    if(rhs.isImmediate()){
                        asmCode = String.format("\taddi %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhs.toString());
                    }else{
                        asmCode = String.format("\tadd %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                    }
                }
                case SUB -> {
                    IRValue lhs = instr.getLHS();
                    IRValue rhs = instr.getRHS();
                    IRVariable result = instr.getResult();
                    VariableToRegister(lhs, i);
                    VariableToRegister(rhs, i);
                    VariableToRegister(result, i);
                    Register lhsReg = registerBMap.getByKey(lhs);
                    Register rhsReg = registerBMap.getByKey(rhs);
                    Register resultReg = registerBMap.getByKey(result);
                    asmCode = String.format("\tsub %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                }
                case MUL -> {
                    IRValue lhs = instr.getLHS();
                    IRValue rhs = instr.getRHS();
                    IRVariable result = instr.getResult();
                    VariableToRegister(lhs, i);
                    VariableToRegister(rhs, i);
                    VariableToRegister(result, i);
                    Register lhsReg = registerBMap.getByKey(lhs);
                    Register rhsReg = registerBMap.getByKey(rhs);
                    Register resultReg = registerBMap.getByKey(result);
                    asmCode = String.format("\tmul %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                }
                case MOV -> {
                    IRValue form = instr.getFrom();
                    IRVariable result = instr.getResult();
                    VariableToRegister(form, i);
                    VariableToRegister(result, i);
                    Register formReg = registerBMap.getByKey(form);
                    Register resultReg = registerBMap.getByKey(result);
                    if(form.isImmediate()){
                        asmCode = String.format("\tli %s, %s", resultReg.toString(), form.toString());
                    }else{
                        asmCode = String.format("\tmv %s, %s", resultReg.toString(), formReg.toString());
                    }
                }
                case RET -> {
                    IRValue returnValue = instr.getReturnValue();
                    Register returnReg = registerBMap.getByKey(returnValue);
                    asmCode = String.format("\tmv a0, %s", returnReg.toString());
                }
                default -> System.out.println("error asm!");
            }

            asmCode += "\t\t#  %s".formatted(instr.toString());
            asmInstructions.add(asmCode);
            i++;

            if(instrKind == InstructionKind.RET){
                break;
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, asmInstructions);
    }
}

