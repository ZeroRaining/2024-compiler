1. **系统架构设计**

* 结构图

  * ```
    └── src
        ├── Compiler.java
        ├── META-INF
        │   └── MANIFEST.MF
        ├── Utils
        │   └── CustomList.java
        ├── arg
        │   └── Arg.java
        ├── backend
        │   ├── BackendPrinter.java
        │   ├── BlockSort.java
        │   ├── DeleteUnusedBlock.java
        │   ├── IrParser.java
        │   ├── PeepHole.java
        │   ├── RegAlloc.java
        │   ├── RegAllocAno.java
        │   ├── RegAllocLinear.java
        │   ├── asmInstr
        │   │   ├── AsmInstr.java
        │   │   ├── asmBinary
        │   │   │   ├── AsmAdd.java
        │   │   │   ├── AsmAnd.java
        │   │   │   ├── AsmBinary.java
        │   │   │   ├── AsmDiv.java
        │   │   │   ├── AsmFAdd.java
        │   │   │   ├── AsmFDiv.java
        │   │   │   ├── AsmFMul.java
        │   │   │   ├── AsmFSub.java
        │   │   │   ├── AsmFeq.java
        │   │   │   ├── AsmFge.java
        │   │   │   ├── AsmFgt.java
        │   │   │   ├── AsmFle.java
        │   │   │   ├── AsmFlt.java
        │   │   │   ├── AsmFneg.java
        │   │   │   ├── AsmMod.java
        │   │   │   ├── AsmMul.java
        │   │   │   ├── AsmOr.java
        │   │   │   ├── AsmSll.java
        │   │   │   ├── AsmSlt.java
        │   │   │   ├── AsmSlti.java
        │   │   │   ├── AsmSltu.java
        │   │   │   ├── AsmSra.java
        │   │   │   ├── AsmSrl.java
        │   │   │   ├── AsmSub.java
        │   │   │   └── AsmXor.java
        │   │   ├── asmBr
        │   │   │   ├── AsmBnez.java
        │   │   │   └── AsmJ.java
        │   │   ├── asmConv
        │   │   │   ├── AsmFtoi.java
        │   │   │   ├── AsmZext.java
        │   │   │   └── AsmitoF.java
        │   │   ├── asmLS
        │   │   │   ├── AsmFld.java
        │   │   │   ├── AsmFlw.java
        │   │   │   ├── AsmFsd.java
        │   │   │   ├── AsmFsw.java
        │   │   │   ├── AsmL.java
        │   │   │   ├── AsmLLa.java
        │   │   │   ├── AsmLa.java
        │   │   │   ├── AsmLd.java
        │   │   │   ├── AsmLw.java
        │   │   │   ├── AsmMove.java
        │   │   │   ├── AsmS.java
        │   │   │   ├── AsmSd.java
        │   │   │   ├── AsmSw.java
        │   │   │   └── LSType.java
        │   │   └── asmTermin
        │   │       ├── AsmCall.java
        │   │       └── AsmRet.java
        │   ├── itemStructure
        │   │   ├── AsmBlock.java
        │   │   ├── AsmFunction.java
        │   │   ├── AsmGlobalVar.java
        │   │   ├── AsmImm12.java
        │   │   ├── AsmImm32.java
        │   │   ├── AsmLabel.java
        │   │   ├── AsmModule.java
        │   │   ├── AsmOperand.java
        │   │   ├── AsmType.java
        │   │   ├── Group.java
        │   │   └── LibFunctionGeter.java
        │   └── regs
        │       ├── AsmFPhyReg.java
        │       ├── AsmFVirReg.java
        │       ├── AsmPhyReg.java
        │       ├── AsmReg.java
        │       ├── AsmVirReg.java
        │       └── RegGeter.java
        ├── debug
        │   └── DEBUG.java
        ├── frontend
        │   ├── ir
        │   │   ├── DataType.java
        │   │   ├── FuncDef.java
        │   │   ├── Use.java
        │   │   ├── Value.java
        │   │   ├── constvalue
        │   │   │   ├── ConstBool.java
        │   │   │   ├── ConstFloat.java
        │   │   │   ├── ConstInt.java
        │   │   │   └── ConstValue.java
        │   │   ├── instr
        │   │   │   ├── Instruction.java
        │   │   │   ├── binop
        │   │   │   │   ├── AShrInstr.java
        │   │   │   │   ├── AddInstr.java
        │   │   │   │   ├── BinaryOperation.java
        │   │   │   │   ├── FAddInstr.java
        │   │   │   │   ├── FDivInstr.java
        │   │   │   │   ├── FMulInstr.java
        │   │   │   │   ├── FRemInstr.java
        │   │   │   │   ├── FSubInstr.java
        │   │   │   │   ├── MulInstr.java
        │   │   │   │   ├── SDivInstr.java
        │   │   │   │   ├── SRemInstr.java
        │   │   │   │   ├── ShlInstr.java
        │   │   │   │   ├── SubInstr.java
        │   │   │   │   └── Swappable.java
        │   │   │   ├── convop
        │   │   │   │   ├── Bitcast.java
        │   │   │   │   ├── ConversionOperation.java
        │   │   │   │   ├── Fp2Si.java
        │   │   │   │   ├── Si2Fp.java
        │   │   │   │   └── Zext.java
        │   │   │   ├── memop
        │   │   │   │   ├── AllocaInstr.java
        │   │   │   │   ├── GEPInstr.java
        │   │   │   │   ├── LoadInstr.java
        │   │   │   │   ├── MemoryOperation.java
        │   │   │   │   └── StoreInstr.java
        │   │   │   ├── otherop
        │   │   │   │   ├── CallInstr.java
        │   │   │   │   ├── EmptyInstr.java
        │   │   │   │   ├── MoveInstr.java
        │   │   │   │   ├── PCInstr.java
        │   │   │   │   ├── PhiInstr.java
        │   │   │   │   └── cmp
        │   │   │   │       ├── Cmp.java
        │   │   │   │       ├── CmpCond.java
        │   │   │   │       ├── FCmpInstr.java
        │   │   │   │       └── ICmpInstr.java
        │   │   │   ├── terminator
        │   │   │   │   ├── BranchInstr.java
        │   │   │   │   ├── JumpInstr.java
        │   │   │   │   ├── ReturnInstr.java
        │   │   │   │   └── Terminator.java
        │   │   │   └── unaryop
        │   │   │       └── FNegInstr.java
        │   │   ├── lib
        │   │   │   ├── FuncGetarray.java
        │   │   │   ├── FuncGetch.java
        │   │   │   ├── FuncGetfarray.java
        │   │   │   ├── FuncGetfloat.java
        │   │   │   ├── FuncGetint.java
        │   │   │   ├── FuncMemset.java
        │   │   │   ├── FuncPutarray.java
        │   │   │   ├── FuncPutch.java
        │   │   │   ├── FuncPutfarray.java
        │   │   │   ├── FuncPutfloat.java
        │   │   │   ├── FuncPutint.java
        │   │   │   ├── FuncStarttime.java
        │   │   │   ├── FuncStoptime.java
        │   │   │   ├── Lib.java
        │   │   │   └── LibFunc.java
        │   │   ├── structure
        │   │   │   ├── BasicBlock.java
        │   │   │   ├── FParam.java
        │   │   │   ├── Function.java
        │   │   │   ├── GlobalObject.java
        │   │   │   ├── Procedure.java
        │   │   │   └── Program.java
        │   │   └── symbols
        │   │       ├── ArrayInitVal.java
        │   │       ├── InitExpr.java
        │   │       ├── SymTab.java
        │   │       └── Symbol.java
        │   ├── lexer
        │   │   ├── Lexer.java
        │   │   ├── Token.java
        │   │   ├── TokenList.java
        │   │   └── TokenType.java
        │   └── syntax
        │       ├── Ast.java
        │       └── Parser.java
        └── midend
            ├── FMHBR.java
            ├── FuncMemorize.java
            ├── RemovePhi.java
            ├── SSA
            │   ├── ArrayFParamMem2Reg.java
            │   ├── ConstBroadcast.java
            │   ├── DFG.java
            │   ├── DeadBlockRemove.java
            │   ├── DeadCodeRemove.java
            │   ├── DetectTailRecursive.java
            │   ├── FI.java
            │   ├── GCM.java
            │   ├── GVN.java
            │   ├── GlobalValueSimplify.java
            │   ├── Mem2Reg.java
            │   ├── MergeBlock.java
            │   ├── MergeGEP.java
            │   ├── OIS.java
            │   ├── OISR.java
            │   ├── PtrMem2Reg.java
            │   ├── RemoveUseLessPhi.java
            │   └── SimplifyBranch.java
            └── loop
                ├── AnalysisLoop.java
                ├── BlockType.java
                ├── LCSSA.java
                ├── Loop.java
                ├── LoopInvariantMotion.java
                ├── LoopRotate.java
                ├── LoopSimplify.java
                ├── LoopUnroll.java
                └── RemoveUseLessLoop.java
    
    ```

2. **关键技术与方法**

- 前端

- 中端

  - SSA

    - ArrayFParamMem2Reg

      ```
      * 针对数组形参的 mem2reg
      * 传入的数组形参实际上是数组首元素的指针，理论上不会被修改，因此 load 的结果就应该是第一次 store 的内容
      * 可以比较轻易地进行 mem2reg
      ```

    - DeadBlockRemove

      ```
      * 删除不必要的的中端代码
      ```

    - DetectTailRecursive

      ```
      * 识别尾递归
      ```

    - DFG

      ```
      * 实现了对函数控制流图（CFG）进行支配树（Dominator Tree）和支配边界（Dominance Frontier）的计算
      ```

    - FI

      ```
       * Function inlining
       * 函数内联
       * 值得注意的是，函数内联并不一定能提高运行效率，有时候一些不常用的代码（冷代码）可能作为函数调用存在会更合适一些
      ```

    - GCM

      ```
       * GCM(Global Code Motion)
       * 全局代码移动（全局代码提升）
      ```

    - GlobalValueSimplify

      ```
       * 全局对象简化，包括将没有被更新过的全局对象直接用初值替代，以及【局部化】
       * 安排在函数内联之后、Mem2Reg 之前
       * 全局对象局部化：
       * 现阶段想法是若一个全局变量只被 main 函数使用过，则将其变为局部变量。
      ```

    - GVN

      ```
      * GVN 通过为每个表达式分配唯一的编号（或标识符），来检测程序中的等价表达式。如果多个表达式具有相同的编号，则这些表达式计算相同的结果，因而可以消除冗余计算。
      *	功能：对传入的函数列表 functions 进行全局值编号优化。
      *	实现：
      	*	首先检查 functions 是否为空，如果为空则抛出 NullPointerException。
      	*	然后，对每个函数 Function，从其入口基本块（BasicBlock）开始执行 dfsIdoms 方法。
      ```

    - MergeGEP

      ```
       * 用来将 GEP 合并，便于判断两个 GEP 是否指向同一个地址，保证一个地址，一次取完（在可合并的情况下）
      ```

    - OIS

      ```
       * Operation instruction simplification
       * 运算指令简化
      ```

    - OISR

      ```
       * Operation Instruction Strength Reduction
       * 运算指令强度消减
      ```

    - PtrMem2Reg

      ```
       * 针对指针的 mem2reg，可以认为是在做别名分析
       * 现在的想法是按照控制树 dfs，保存对于一个地址的赋值记录，如果 dom 分叉（直接控制块不止一个）则清空当前列表，以避免多重定义导致冲突。
       * 对于 call 需要特殊处理，函数中可能出现修改指针指向内存内容的操作。
       * 还有一个前置任务是要合并 GEP，避免相同指针的表现形式不同导致错误删除和不当替换
       * 对于只定义过一次的值可以直接替换
      ```

    - FuncMemorize

      ```
       * （递归）函数记忆化，通过全局数组减少递归运算
       * 因为需要跳转到结束块，所以必须放在合并块之前
       * 为了减少哈希计算，决定对于要操作的递归函数，要在递归结束条件判断之后才做哈希
       * 因为没有控制流图实在不知道怎么找返回值类型了，决定将这个减少哈希计算的操作放到后面 FMHBR 里
       * todo: 现在找递归结束条件的原则是直接返回常数或者参数，但是其实更合适的应该是去找最后一个能支配所有递归调用的块，在它那做哈希
      ```

      

- 后端

  - IrParser
    - IrParser的主要任务是将IR代码翻译为RISC-V代码。对于大多指令，在不考虑优化的情况下可对应进行翻译。较为特殊的有以下几个要点：
      - 栈空间的分配。我们的设计是每个函数维护自己的栈，从栈顶到栈底依次存储函数内Call指令8个以后的参数（默认使用a0-a7进行传参），执行函数过程中使用alloca指令申请的栈空间，寄存器溢出占用的栈空间，ra。由于寄存器溢出占用的栈空间在寄存器分配之前无法计算，因此在IrParser中会计算剩下三个部分的栈空间大小，最后在寄存器分配部分完善栈空间的申请。
      - 指令翻译。通过HashMap建立IR中虚拟寄存器与RISC-V中虚拟寄存器的映射关系。在翻译过程中尽量采用基本指令，从而避免拓展指令对寄存器值进行意外的修改，后续优化也会更加灵活。
      - 指令降级。对乘、除、模等指令，转化为CPU周期更小的移位、加减等指令。
  - RegAlloc, RegAllocAno, RegAllocLinear
    - 后端我们一共写了三个版本的寄存器分配，前两个是图着色，最后一个是线性扫描分配
      - 第一版图着色我们是参考 虎书 上的伪代码实现的，实现了数据流分析、化简、合并和溢出，我们先对浮点寄存器进行分配，然后对整数寄存器进行分配，同时我们也参照riscv的调用规约，做了调用者保存和被调用者保存，还做了进入函数后的栈分配
      - 第二版的图着色是在第一版的基础上做了更细致的分配，在每一轮分配时我们会把虚拟寄存器分成两类，一类是跨函数调用活跃的虚拟寄存器，另一类是它的补集，我们会有先对跨函数调用活跃的寄存器进行分配，只会为它分配S类寄存器，接着我们会对另一类进行分配，寄存器分配的顺序是T，A，S。
      - 特殊的寄存器：t0, 我们是专门用来做地址加载的，a0我们是专门用来做传参和加载返回值的
      - 合理地选择着色顺序：我们通过根据循环的深度为寄存器附上权重，再按照权重和冲突数组合选择从图中抽出的顺序
      - 合理地选择溢出：溢出的顺序也是像着色顺序一样