package backend;

import Utils.CustomList;
import backend.asmInstr.asmLS.AsmL;
import backend.asmInstr.asmLS.AsmMove;
import backend.asmInstr.asmLS.AsmS;
import backend.asmInstr.asmTermin.AsmCall;
import backend.itemStructure.AsmBlock;
import backend.itemStructure.AsmFunction;
import backend.itemStructure.AsmImm12;
import backend.itemStructure.AsmModule;

import java.util.ArrayList;

public class PeepHole {
    public static void run(AsmModule module) {
        deleteStoreAndLoad(module);
    }

    public static void deleteStoreAndLoad(AsmModule module) {
        for (AsmFunction asmFunction : module.getFunctions()) {
            for (CustomList.Node node : asmFunction.getBlocks()) {
                AsmBlock block = (AsmBlock) node;
                for (CustomList.Node instrNode : block.getInstrs()) {
                    if (instrNode instanceof AsmS asmS) {
                        if (instrNode.getNext() != null && instrNode.getNext() instanceof AsmL asmL) {
                            if (asmL.getOffset() == null) {
                                //考虑到有La,LLa,Move的情况
                                continue;
                            }
                            if (asmS.getSrc() == asmL.getDst() && ((AsmImm12) (asmS.getOffset())).getValue() == ((AsmImm12) (asmL.getOffset())).getValue() && asmS.getAddr() == asmL.getSrc()) {
                                //asmS.removeFromList();
                                asmL.removeFromList();
                            }
                        }
                    }
                }
            }
        }
    }
}
