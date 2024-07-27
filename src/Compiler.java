import arg.Arg;
import backend.*;
import backend.itemStructure.AsmModule;
import frontend.ir.structure.Function;
import frontend.ir.structure.Program;
import frontend.lexer.Lexer;
import frontend.lexer.TokenList;
import frontend.syntax.Ast;
import frontend.syntax.Parser;
import midend.RemovePhi;
import midend.SSA.*;

import java.io.*;
import java.util.HashSet;

public class Compiler {
    public static void main(String[] args) throws IOException {
        // 解析命令行
        Arg arg = Arg.parse(args);
        BufferedInputStream source = new BufferedInputStream(arg.getSrcStream());
        BufferedWriter output = new BufferedWriter(arg.getAsmWriter());
        
        // 准备计时
        long startTime = 0;
        long optimizeStartTime = 0;
        long optimizeEndTime = 0;
        if (arg.toTime()) {
            startTime = System.currentTimeMillis();
        }
        
        // 词法分析，得到 TokenList
        TokenList tokenList = Lexer.getInstance().lex(source);
        // 语法分析，得到 AST
        Ast ast = new Parser(tokenList).parseAst();
        // 生成 IR
        Program program = new Program(ast);
        
        if (arg.getOptLevel() == 1) {
            FI.doFI(program.getFunctionList());     // todo: 函数内联，未来可能要加一些限制，不一定全内联
        }
        
        program.removeUselessFunc();
        
        HashSet<Function> functions = new HashSet<>(program.getFunctions().values());
        DeadBlockRemove.execute(functions);
        DFG.doDFG(functions);

        // 开启优化
        if (arg.toTime()) {
            optimizeStartTime = System.currentTimeMillis();
        }
        if (arg.getOptLevel() == 1) {
            Mem2Reg.doMem2Reg(functions);
            int times = 2;
            int cnt = 0;
            while (cnt < times) {
                DeadCodeRemove.doDeadCodeRemove(functions);
                OIS.doOIS(functions);
//                OISR.doOISR(functions);
                GVN.doGVN(functions);
                SimplifyBranch.execute(functions);
                MergeBlock.execute(functions);
                DeadBlockRemove.execute(functions);
                RemoveUseLessPhi.execute(functions);
                cnt++;
                if (cnt < times) {
                    DFG.doDFG(functions);
                }
            }
        }
        if (arg.toTime()) {
            optimizeEndTime = System.currentTimeMillis();
        }

        // 打印 IR
        if (arg.toPrintIR()) {
//            Function.blkLabelReorder();
            
            BufferedWriter irWriter = new BufferedWriter(arg.getIrWriter());
            program.printIR(irWriter);
            irWriter.close();
        }
        
        if (arg.getOptLevel() == 1 && !arg.toSkipBackEnd()) {
            RemovePhi.phi2move(functions);
        }
        
        // 运行后端
        if (!arg.toSkipBackEnd()) {
            IrParser parser = new IrParser(program);
            AsmModule asmModule = parser.parse();
            RegAlloc alloc = RegAlloc.getInstance(parser.downOperandMap);
            alloc.run(asmModule);
//            RegAllocAno allocAno = RegAllocAno.getInstance(parser.downOperandMap);
//            allocAno.run(asmModule);
//            RegAllocLinear alloc = RegAllocLinear.getInstance(parser.downOperandMap);
//            alloc.debug(asmModule);
            BackendPrinter backendPrinter = new BackendPrinter(asmModule, true, output);
            backendPrinter.printBackend();
        }
        
        // 计算运行时间
        if (arg.toTime()) {
            long endTime = System.currentTimeMillis();
            long runTime = endTime - startTime;
            long optimizingTime = optimizeEndTime - optimizeStartTime;
            
            System.out.println("runTime: " + runTime + "ms");
            System.out.println("optimizingTime: " + optimizingTime + "ms");
        }
    }
}
