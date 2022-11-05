package CodeGen;

import java.util.HashMap;

public class ReturnInfo {
    public Integer eip;
    public HashMap<String, Variable> varTable;
    public Integer stackPtr;
    public Integer paramsCount;
    public Integer callArgsCount;
    public Integer nowArgsCount;

    public ReturnInfo(Integer eip, HashMap<String, Variable> varTable, Integer stackPtr, Integer paramsCount, Integer callArgsCount, Integer nowArgsCount) {
        this.eip = eip;
        this.varTable = varTable;
        this.stackPtr = stackPtr;
        this.paramsCount = paramsCount;
        this.callArgsCount = callArgsCount;
        this.nowArgsCount = nowArgsCount;
    }
}
