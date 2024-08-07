package midend;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.binop.AddInstr;
import frontend.ir.instr.binop.MulInstr;
import frontend.ir.instr.binop.SRemInstr;
import frontend.ir.instr.convop.Fp2Si;
import frontend.ir.instr.memop.GEPInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.FParam;
import frontend.ir.structure.Function;
import frontend.ir.symbols.ArrayInitVal;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;

import java.util.ArrayList;
import java.util.Objects;

/**
 * （递归）函数记忆化，通过全局数组减少递归运算
 * 因为需要跳转到结束块，所以必须放在合并块之前
 */
public class FuncMemorize {
    private static final int hash_base = 53;
    private static final int hash_mod = 1007;
    
    public static void execute(ArrayList<Function> functions, SymTab globalSymTab) {
        for (Function function : functions) {
            if (function.canBeMemorized()) {
                // 建立两个全局数组，data 和 used
                DataType funcRetType = function.getDataType();
                ArrayList<Integer> globalDataLimList = new ArrayList<>();
                globalDataLimList.add(hash_mod);
                ArrayInitVal globalDataInit = new ArrayInitVal(funcRetType, globalDataLimList, true);
                Symbol globalData = new Symbol("global_data_" + function.getName(), funcRetType,
                        globalDataLimList, false, true, globalDataInit);
                globalSymTab.addSym(globalData);
                
                ArrayList<Integer> globalUsedLimList = new ArrayList<>();
                globalUsedLimList.add(hash_mod);
                ArrayInitVal globalUsedInit = new ArrayInitVal(DataType.INT, globalUsedLimList, true);
                Symbol globalUsed = new Symbol("global_used_" + function.getName(), DataType.INT,
                        globalUsedLimList, false, true, globalUsedInit);
                globalSymTab.addSym(globalUsed);
                
                // 建立两个基本块，并准备两个常数对象
                BasicBlock bbHash = new BasicBlock(0, function.getAndAddBlkIndex());
                BasicBlock bbRet  = new BasicBlock(0, function.getAndAddBlkIndex());
                
                ConstInt baseVal = new ConstInt(hash_base);
                ConstInt modVal  = new ConstInt(hash_mod);
                
                // 将所有的参数转化为整数以备哈希
                ArrayList<Value> castArgs = new ArrayList<>();
                for (FParam fParam : function.getFParams()) {
                    if (fParam.getDataType() == DataType.INT) {
                        castArgs.add(fParam);
                    } else {
                        Fp2Si cast = new Fp2Si(function.getAndAddRegIndex(), fParam);
                        bbHash.addInstruction(cast);
                        castArgs.add(cast);
                    }
                }
                
                // 哈希计算
                Value hashVal = castArgs.get(0);
                if (castArgs.size() == 1) {
                    hashVal = new SRemInstr(function.getAndAddRegIndex(), hashVal, modVal);
                    bbHash.addInstruction((Instruction) hashVal);
                } else {
                    for (int i = 1; i < castArgs.size(); i++) {
                        hashVal = new MulInstr(function.getAndAddRegIndex(), hashVal, baseVal);
                        bbHash.addInstruction((Instruction) hashVal);
                        hashVal = new AddInstr(function.getAndAddRegIndex(), hashVal, castArgs.get(i));
                        bbHash.addInstruction((Instruction) hashVal);
                        hashVal = new SRemInstr(function.getAndAddRegIndex(), hashVal, modVal);
                        bbHash.addInstruction((Instruction) hashVal);
                    }
                }
                
                // 找到数组对应的位置并检查是否已经计算过
                ArrayList<Value> indexList = new ArrayList<>();
                indexList.add(hashVal);
                GEPInstr dataPtr = new GEPInstr(function.getAndAddRegIndex(), new ArrayList<>(indexList), globalData);
                GEPInstr usedPtr = new GEPInstr(function.getAndAddRegIndex(), new ArrayList<>(indexList), globalData);
                bbHash.addInstruction(dataPtr);
                bbHash.addInstruction(usedPtr);
                
                
            }
        }
    }
}
