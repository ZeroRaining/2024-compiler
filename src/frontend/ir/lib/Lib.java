package frontend.ir.lib;

import frontend.ir.Value;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.structure.BasicBlock;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Lib {
    private static final Lib instance = new Lib();
    private final HashMap<String, Class<? extends LibFunc>> allFunctions = new HashMap<>();
    private final HashSet<Class<? extends LibFunc>> usedFunc = new HashSet<>();
    private final HashSet<LibFunc> usedFuncInstance = new HashSet<>();
    
    private Lib() {
        // I/O
        allFunctions.put("getint",      FuncGetint.class);      // int getint();
        allFunctions.put("getch",       FuncGetch.class);       // int getch();
        allFunctions.put("getfloat",    FuncGetfloat.class);    // float getfloat();
        allFunctions.put("getarray",    null);                  // int getarray(int[]);         todo
        allFunctions.put("getfarray",   null);                  // int getfarray(float[]);      todo
        allFunctions.put("putint",      FuncPutint.class);      // void putint(int);
        allFunctions.put("putch",       FuncPutch.class);       // void putch(int);
        allFunctions.put("putfloat",    FuncPutfloat.class);    // void putfloat(float);
        allFunctions.put("putarray",    null);                  // void putarray(int, int[]);   todo
        allFunctions.put("putfarray",   null);                  // void putfarray(int, float[]);todo
        allFunctions.put("putf",        null);                  // void putf(<fmt>, int, ...);  todo
        // Timing
        allFunctions.put("starttime",   null);  // void starttime()     todo
        allFunctions.put("stoptime",    null);  // void stoptime()      todo
    }
    
    public static Lib getInstance() {
        return instance;
    }
    
    public CallInstr makeCall(int result, String funcName, List<Value> rParams, BasicBlock curBlock) {
        Class<? extends LibFunc> funcClass = allFunctions.get(funcName);
        if (funcClass == null) {
            return null;
        }
        LibFunc func;
        try {
            func = funcClass.getDeclaredConstructor(List.class).newInstance(rParams);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (usedFunc.add(funcClass)) {
            usedFuncInstance.add(func);
        }
        return func.makeCall(result, rParams, curBlock);
    }
    
    public void declareUsedFunc(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        
        if (usedFuncInstance.isEmpty()) {
            return;
        }
        
        for (LibFunc func : usedFuncInstance) {
            writer.append(func.printDeclaration()).append("\n");
        }
        
        writer.append("\n");
    }
}
