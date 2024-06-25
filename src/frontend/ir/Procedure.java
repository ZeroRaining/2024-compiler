package frontend.ir;

import frontend.ir.instr.ReturnInstr;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Procedure {
    private final ArrayList<BasicBlock> basicBlocks = new ArrayList<>();
    private int nextSymIdx = 0;
    
    public Procedure(DataType returnType, List<Ast.FuncFParam> fParams, Ast.Block block) {
        if (fParams == null || block == null) {
            throw new NullPointerException();
        }
        
        basicBlocks.add(new BasicBlock(fParams.size()));
        
        for (Ast.BlockItem item : block.getItems()) {
            if (item instanceof Ast.Stmt) {
                if (item instanceof Ast.Return) {
                    Ast.Exp returnValue = ((Ast.Return) item).getReturnValue();
                    if (returnValue == null) {
                        basicBlocks.get(basicBlocks.size() - 1).addInstruction(new ReturnInstr(returnType));
                    } else {
                        switch (returnValue.checkConstType()) {
                            case INT:
                                Integer retI = returnValue.getConstInt();
                                assert retI != null;
                                basicBlocks.get(basicBlocks.size() - 1).addInstruction(new ReturnInstr(returnType, retI));
                                break;
                            case FLOAT:
                                Float retF = returnValue.getConstFloat();
                                assert retF != null;
                                basicBlocks.get(basicBlocks.size() - 1).addInstruction(new ReturnInstr(returnType, retF));
                                break;
                            default:
                                // 返回值非常量
                                // todo
                        }
                    }
                    
                }
            }
        }
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        // todo 首先应该挖个坑给参数埋起来（分配内存）
        for (BasicBlock block : basicBlocks) {
            writer.append(Integer.toString(block.getLabel())).append(":\n");
            block.printIR(writer);
        }
    }
}
