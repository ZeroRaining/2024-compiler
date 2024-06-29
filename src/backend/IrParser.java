package backend;

import backend.itemStructure.*;
import frontend.ir.BasicBlock;
import frontend.ir.Program;
import frontend.ir.symbols.Symbol;
import frontend.ir.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class IrParser {
    private Program program;
    private AsmModule asmModule;
    private HashMap<Symbol, AsmGlobalVar> gvMap;
    private HashMap<Function, AsmFunction> funcMap;
    private HashMap<BasicBlock, AsmBlock> blockMap;


    public IrParser(Program program) {
        this.program = program;
        this.asmModule = new AsmModule();
        this.gvMap = new HashMap<>();
        setLibFunctions();
    }

    public AsmModule parse(Program program) {
        parseGlobalVars();
        parseFunctions();
    }

    private void parseGlobalVars() {
        HashSet<Symbol> globalVars = program.getGlobalVars();
        for (Symbol globalVar : globalVars) {
            AsmGlobalVar asmGlobalVar = parseGlobalVar(globalVar);
            asmModule.addGlobalVar(asmGlobalVar);
            gvMap.put(globalVar, asmGlobalVar);
        }
    }

    private AsmGlobalVar parseGlobalVar(Symbol globalVar) {
        //TODO: 指针类型？Type的isPointerType？
        AsmType type = globalVar.getAsmType();
        ArrayList<Integer> items = new ArrayList<>();
        if (type == AsmType.INT) {
            int intVal = globalVar.getValue(AsmType.INT).intValue();
            items.add(intVal);
        } else if (type == AsmType.FLOAT) {
            float floatVal = globalVar.getValue(AsmType.FLOAT).floatValue();
            items.add(Float.floatToIntBits(floatVal));
        } else if (type == AsmType.ARRAY) {
            //TODO:暂时还未完成数组赋值
        }
        if (items.isEmpty()) {
            //TODO:Array有待实现
            int offsetSize = (type == AsmType.ARRAY) ? 4 * globalVar.getArraySize() : 4;
            return new AsmGlobalVar(globalVar.getName(), offsetSize);
        } else {
            return new AsmGlobalVar(globalVar.getName(), items);
        }
    }

    private void parseFunctions() {
        createMaps();
    }

    private void createMaps() {
        for (Function f : program.getFunctions().values()) {
            AsmFunction asmFunction = new AsmFunction(f.getName(), f.isLib());
            asmModule.addFunction(asmFunction);
            funcMap.put(f, asmFunction);
            //TODO:双向链表为什么需要？有待商榷
            int blockIndex = 0;
            for (BasicBlock bb : f.getBasicBlocks()) {
                AsmBlock asmBlock = new AsmBlock(blockIndex++);
                asmFunction.addBlock(asmBlock);
                blockMap.put(bb, asmBlock);
            }

        }
    }

    private void setLibFunctions() {
        //TODO:设置库函数
    }


}
