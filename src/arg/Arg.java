package arg;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Arg {
    //TODO:是否需要处理命令行参数的异常情况？
    private final String asmFilename;
    private final String srcFilename;
    private final String llFilename;
    private final int optLevel;
    private final boolean printIR;
    private final FileInputStream srcStream;//源代码输入流
    private final OutputStream asmStream;//汇编代码输出流
    private final FileWriter irWriter;//汇编代码输出流

    private Arg(String src, String asm, int optimize) throws IOException {
        this.asmFilename = asm;
        this.srcFilename = src;
        this.optLevel    = optimize;
        this.printIR     = false;
        this.llFilename  = null;
        this.irWriter    = null;

        this.srcStream   = new FileInputStream(srcFilename);
        this.asmStream   = Files.newOutputStream(Paths.get(asmFilename));
    }
    
    private Arg(String src, String asm, String ll, int optimize) throws IOException {
        this.asmFilename = asm;
        this.srcFilename = src;
        this.optLevel    = optimize;
        this.printIR     = true;
        this.llFilename  = ll;
        
        this.irWriter    = new FileWriter(llFilename);
        this.srcStream   = new FileInputStream(srcFilename);
        this.asmStream   = Files.newOutputStream(Paths.get(asmFilename));
    }

    public static Arg parse(String[] args) throws IOException {
        String src = "in.sy";
        String asm = "asm.out";
        String ll  = null;
        int optLevel = 0;
        boolean printIR = false;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains(".sy")) {
                src = arg;
                continue;
            }
            if (arg.equals("-o")) {
                asm = args[++i];
                continue;
            }
            if (arg.matches("-O[0-9]")) {
                optLevel = Integer.parseInt(arg.substring(2));
                continue;
            }
            if (arg.equals("-ll")) {
                ll = args[++i];
                printIR = true;
            }
        }
        
        if (printIR) {
            return new Arg(src, asm, ll, optLevel);
        } else {
            return new Arg(src, asm, optLevel);
        }
    }
    
    public FileInputStream getSrcStream() {
        return srcStream;
    }
    
    public int getOptLevel() {
        return optLevel;
    }
    
    public boolean toPrintIR() {
        return printIR;
    }
    
    public OutputStream getAsmStream() {
        return asmStream;
    }
    
    public FileWriter getIrWriter() {
        return irWriter;
    }
}
