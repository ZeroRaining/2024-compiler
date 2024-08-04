# Compiler2024-SubmarineCompiler

## 命令行参数：

### 标准形式：

- 功能测试：`compiler -S -o testcase.s testcase.sy`
- 性能测试：`compiler -S -o testcase.s testcase.sy -O1`

（注意：测评时命令中的testcase.sy和testcase.s会被替换为相应文件的绝对路径，系统会保证编译器对这些文件有相应的读写权限,存放测试用例的目录是只读的）

### 其它功能：

- 打印 IR ：`compiler -S testcase.sy -ll out.ll`
- 跳过后端：`-mid`
- 显示运行时间：`-time`

### 说明：
 
- `-S` 暂时没用，我也不知道这个选项是干什么的；
- 选项之间互不干预，只有 `-o` 和 `-ll` 与下一个参数绑定（作为输出文件名）；
- 输入文件名的识别标准是路径中包含 `.sy`；
- 目前优化等级只有 0 （默认）和 1，因为也没做多少优化
- 优化等级 0 也有优化，但是少一些