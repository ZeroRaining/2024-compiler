package backend;

import Utils.CustomList;
import backend.itemStructure.AsmBlock;
import backend.itemStructure.AsmFunction;
import backend.itemStructure.AsmGlobalVar;
import backend.itemStructure.AsmModule;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class BackendPrinter {
    private AsmModule module;
    public BackendPrinter(AsmModule module) {
        this.module = module;
    }
    public void printBackend() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("output.s"));
        //打印全局变量
        ArrayList<AsmGlobalVar> globalVars = module.getGlobalVars();
        for (AsmGlobalVar globalVar : globalVars) {
            writer.write(globalVar.toString());
            writer.newLine();
        }
        //打印函数
        ArrayList<AsmFunction> functions = module.getFunctions();
        for (AsmFunction function : functions) {
            if(function.getName().equals("main")) {
                writer.write(".section\t.text.startup");
                writer.newLine();
                writer.write(".align\t1");
                writer.newLine();
                writer.write(".globl\tmain");
                writer.newLine();
            } else {
                writer.write(".section\t.text");
                writer.newLine();
                writer.write(".align\t1");
                writer.newLine();
            }
            writer.write(function.getName() + ":");
            writer.newLine();
            CustomList blocks = function.getBlocks();
            for(CustomList.Node node : blocks) {
                AsmBlock block = (AsmBlock) node;
                writer.write(function.getName()+"_"+block.getIndex() + ":");
                writer.newLine();
                for(CustomList.Node instrNode : block.getInstrs()) {
                    writer.write("\t" + instrNode.toString());
                    writer.newLine();
                }
            }
        }
        writer.close();
    }
}
