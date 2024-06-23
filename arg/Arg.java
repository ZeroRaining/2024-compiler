package arg;
import java.io.*;

public class Arg {
    //TODO:是否需要处理命令行参数的异常情况？
    public final String asmFilename;
    public final String srcFilename;
    public final int optLevel;

    public final FileInputStream srcStream;//源代码输入流
    public final OutputStream asmStream;//汇编代码输出流


    public Arg(String src, String asm, int optimize) throws FileNotFoundException {
        this.asmFilename = asm;
        this.srcFilename = src;
        this.optLevel = optimize;

        this.srcStream = new FileInputStream(srcFilename);
        this.asmStream = new FileOutputStream(asmFilename);
    }

    public static Arg parse(String[] args) throws FileNotFoundException {
        String asm = args[2];
        String src = args[3];
        int optLevel = (args.length == 4) ? 0 : 1;

        return new Arg(src, asm, optLevel);
    }
}
//
