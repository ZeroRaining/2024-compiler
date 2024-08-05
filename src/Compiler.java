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
import midend.loop.AnalysisLoop;
import midend.loop.LCSSA;
import midend.loop.LoopUnroll;
import midend.loop.LoopInvariantMotion;

import java.io.*;
import java.util.ArrayList;

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
        
        // 中端优化开始
        if (arg.toTime()) { optimizeStartTime = System.currentTimeMillis(); }
        
        // 函数内联，之后删除没用的函数定义
        FI.execute(program.getFunctionList());
        program.removeUselessFunc();
        // 全局变量简化，之后删掉没用的全局变量定义
        GlobalValueSimplify.execute(program.getGlobalVars());
        program.deleteUselessGlobalVars();
        
        // 获取函数列表用于优化
        ArrayList<Function> functions = program.getFunctionList();
        
        // 删除没用的基本块
        DeadBlockRemove.execute(functions);
        
        // 构建初版 DFG
        DFG.execute(functions);
        
        // 简化代码
        Mem2Reg.execute(functions);
        ArrayFParamMem2Reg.execute(functions);
        
        // 循环优化，当前仅在性能测试时开启
        if (arg.getOptLevel() == 1) {
            DFG.execute(functions);
            AnalysisLoop.execute(functions);
            LCSSA.execute(functions);
            LoopUnroll.execute(functions);
        }
        
        DeadCodeRemove.execute(functions);
        OIS.execute(functions);
        GVN.execute(functions);
        
        SimplifyBranch.execute(functions);
        //合并删减块
        MergeBlock.execute(functions, false);
        DeadBlockRemove.execute(functions);
        RemoveUseLessPhi.execute(functions);
        //循环分析
//        DFG.execute(functions);
//        AnalysisLoop.execute(functions);
//        //LCSSA.execute(functions);
//        LoopInvariantMotion.execute(functions);
        
        // second
        DFG.execute(functions);
        DeadCodeRemove.execute(functions);
        OIS.execute(functions);
        GVN.execute(functions);
        SimplifyBranch.execute(functions);
        MergeBlock.execute(functions, true);
        DeadBlockRemove.execute(functions);
        RemoveUseLessPhi.execute(functions);
        
        // third
        DFG.execute(functions);
        PtrMem2Reg.execute(functions);
        DeadCodeRemove.execute(functions);
        OIS.execute(functions);
        GVN.execute(functions);
        SimplifyBranch.execute(functions);
        MergeBlock.execute(functions, true);
        DeadBlockRemove.execute(functions);
        RemoveUseLessPhi.execute(functions);
        
        DFG.execute(functions);
        AnalysisLoop.execute(functions);


        if (arg.toTime()) { optimizeEndTime = System.currentTimeMillis(); }
        // 中端优化结束

        // 打印 IR
        if (arg.toPrintIR()) {
//            Function.blkLabelReorder();
            BufferedWriter irWriter = new BufferedWriter(arg.getIrWriter());
            program.printIR(irWriter);
            irWriter.close();
        }
        
        // 中后端衔接部分
        if (!arg.toSkipBackEnd()) {
            DetectTailRecursive.detect(functions);
            RemovePhi.phi2move(functions);
        }
        
        // 运行后端
        if (!arg.toSkipBackEnd()) {
            IrParser parser = new IrParser(program);
            AsmModule asmModule = parser.parse();
//            RegAlloc alloc = RegAlloc.getInstance(parser.downOperandMap);
//            alloc.run(asmModule);
            RegAllocAno allocAno = RegAllocAno.getInstance(parser.downOperandMap);
            allocAno.run(asmModule);
//            RegAllocLinear alloc = RegAllocLinear.getInstance(parser.downOperandMap);
//            alloc.debug(asmModule);
            DeleteUnusedBlock.run(asmModule);
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
