package CodeGen;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class VM {
    public ArrayList<PCode> PCodes;
    public int p = 0;
    public int mainFuncAddress;

    public static ArrayList<String> toPrint = new ArrayList<>();
    public FileWriter writer;
    public Scanner scanner;

    public ArrayList<ReturnInfo> returnInfos = new ArrayList<>();
    public ArrayList<Integer> st = new ArrayList<>();
    public HashMap<String, Variable> varTable = new HashMap<>();
    public HashMap<String, Function> funcTable = new HashMap<>();
    public HashMap<String, Integer> labelTable = new HashMap<>();

    public VM(ArrayList<PCode> PCodes, FileWriter writer, Scanner scanner) {
        this.PCodes = PCodes;
        this.writer = writer;
        this.scanner = scanner;

        for (int i = 0 ; i < PCodes.size() ; i++) {
            PCode code = PCodes.get(i);

            // Address of the main Func
            if (code.kind.equals(PCodeKind.MAIN)) {
                mainFuncAddress = i;
            }

            // Functions
            if (code.kind.equals(PCodeKind.FUNC)) {
                funcTable.put((String) code.val1, new Function(i, (int) code.val2));
            }

            // Labels
            if (code.kind == PCodeKind.LABEL) {
                labelTable.put((String) code.val1, i);
            }
        }
    }

    public void push(int i) {
        st.add(i);
    }

    public int pop() {
        return st.remove(st.size() - 1);
    }

    public Variable getVariable(String identifier) {
        if (varTable.containsKey(identifier)) {
            return varTable.get(identifier);
        } else {
            return returnInfos.get(0).varTable.get(identifier);
        }
    }

    public int getAddress(Variable variable, int kindInt) {
        int addr = 0;
        int n = variable.dimensions - kindInt;
        if (n == 0) {
            addr = variable.index;
        } else if (n == 1) {
            int row = pop();
            if (variable.dimensions == 1) {
                addr = variable.index + row;
            } else {
                addr = variable.index + variable.secondDim * row;
            }
        } else if (n == 2) {
            int column = pop();
            int row = pop();
            addr = variable.index + variable.secondDim * row + column;
        }

        return addr;
    }

    public void run(){
        int callArgsCount = 0;
        int nowArgsCount = 0;
        boolean flagMain = false;
        ArrayList<Integer> realParams = new ArrayList<>();
        for(;p < PCodes.size(); p++) {
//            System.out.println(p);
            PCode code = PCodes.get(p);

            switch (code.kind) {

                case VARIABLE: {
                    Variable variable = new Variable(st.size());
                    varTable.put((String) code.val1, variable);
                }
                break;

                case PUSH : {
                    if (code.val1 instanceof Integer) {
                        push((Integer) code.val1);
                    }
                }
                break;

                case POP : {
                    int val = pop();
                    int addr = pop();
                    st.set(addr, val);
                }
                break;

                case ADD : {
                    push(pop() + pop());
                }
                break;

                case SUBTRACT: {
                    push (-(pop() - pop()));
                }
                break;

                case DIVIDE: {
                    int operand = pop();
                    int secondOperand = pop();
                    push(secondOperand / operand);
                }
                break;

                case MOD : {
                    int operand = pop();
                    int secondOperand = pop();
                    push(secondOperand % operand);
                }
                break;

                case MULTIPLY: {
                    push(pop() * pop());
                }
                break;

                case NEG : {
                    push(-pop());
                }
                break;

                case POS : {
                    push(pop());
                }
                break;

                case LABEL: {}
                break;

                case CEQ: {
                    if (pop() == pop()) {
                        push(1);
                    } else {
                        push(0);
                    }
                }
                break;

                case CNE: {
                    if (pop() != pop() ){
                        push(1);
                    } else{
                        push(0);
                    }
                }
                break;

                case CLT: {
                    if (pop() > pop()) {
                        push(1);
                    } else {
                        push(0);
                    }
                }
                break;

                case CLE: {
                    if (pop() >= pop()) {
                        push(1);
                    } else {
                        push(0);
                    }
                }
                break;

                case CGT: {
                    if (pop() < pop()) {
                        push(1);
                    } else {
                        push(0);
                    }
                }
                break;

                case CGE: {
                    if (pop() <= pop() ) {
                        push(1);
                    } else {
                        push(0);
                    }
                }
                break;

                case BITAND: {
                    int b = pop();
                    int a = pop();
                    push(b & a);
                }
                break;

                case AND: {
                    int b = pop();
                    int a = pop();
                    if (a != 0 && b != 0) {
                        push(1);
                    } else {
                        push(0);
                    }
                }
                break;

                case OR: {
                    int b = pop();
                    int a = pop();
                    if (a != 0 || b != 0) {
                        push(1);
                    } else {
                        push(0);
                    }
                }
                break;

                case NOT: {
                    if (pop() == 0) {
                        push(1);
                    } else {
                        push(0);
                    }
                }
                break;

                case JZ: {
                    if (st.get(st.size() - 1) == 0) {
                        p = labelTable.get((String) code.val1);
                    }
                }
                break;

                case JNZ: {
                    if (st.get(st.size() -1 ) != 0) {
                        p = labelTable.get((String) code.val1);
                    }
                }
                break;

                case JMP: {
                    p = labelTable.get((String) code.val1);
                }
                break;

                case FUNC: {
                    if (!flagMain) {
                        p = mainFuncAddress - 1;
                    }
                }
                break;

                case MAIN : {
                    flagMain = true;
                    returnInfos.add(new ReturnInfo(PCodes.size(), varTable, st.size() -1, 0, 0, 0));
                    varTable = new HashMap<>();
                }

                case FUNCEND: {}
                break;

                case PARAM: {
                    Variable param = new Variable(realParams.get(realParams.size() - callArgsCount + nowArgsCount));
                    int n = (int) code.val2;
                    param.dimensions = n;
                    if (n == 2) {
                        param.secondDim = pop();
                    }
                    varTable.put((String) code.val1, param);
                    nowArgsCount += 1;
                    if (nowArgsCount == callArgsCount) {
                        realParams.subList(realParams.size() - callArgsCount, realParams.size()).clear();
                    }
                }
                break;

                case RETURN: {
                    int n = (int) code.val1;
                    ReturnInfo returnInfo = returnInfos.remove(returnInfos.size() - 1);
                    p = returnInfo.eip;
                    varTable = returnInfo.varTable;
                    callArgsCount = returnInfo.callArgsCount;
                    nowArgsCount = returnInfo.nowArgsCount;

                    if (n == 1) {
                        st.subList(returnInfo.stackPtr + 1 - returnInfo.paramsCount, st.size() - 1).clear();
                    } else {
                        st.subList(returnInfo.stackPtr + 1 - returnInfo.paramsCount, st.size()).clear();
                    }
                }
                break;

                case CALL : {
                    Function function = funcTable.get((String) code.val1);
                    returnInfos.add(new ReturnInfo(p, varTable, st.size() - 1, function.argCount, function.argCount, nowArgsCount));
                    p = function.index;

                    varTable= new HashMap<>();
                    callArgsCount = function.argCount;
                    nowArgsCount = 0;
                }
                break;

                case REALPARAM: {
                    int n = (int) code.val1;
                    if (n == 0) {
                        realParams.add(st.size() - 1);
                    } else {
                        realParams.add(st.get(st.size() - 1));
                    }
                }
                break;

                case GETINT : {
                    push(scanner.nextInt());
                }
                break;

                case PRINT : {
                    String str = (String) code.val1;
                    int n = (int) code.val2;

                    StringBuilder stringBuilder = new StringBuilder();
                    ArrayList<Integer> params = new ArrayList<>();

                    for (int i = 0; i < n; i++) {
                        params.add(pop());
                    }

                    int index = n - 1;
                    for (int i = 0; i < str.length(); i++) {
                        if (i + 1 < str.length() && str.charAt(i) == '%' && str.charAt(i + 1) == 'd')  {
                            stringBuilder.append(params.get(index).toString());
                            index -= 1;
                            i += 1;
                            continue;
                        }
                        stringBuilder.append(str.charAt(i));
                    }
                    toPrint.add(stringBuilder.substring(1, stringBuilder.length() - 1));
                }
                break;

                case VALUE : {
                    Variable variable = getVariable((String) code.val1);
                    int n = (int) code.val2;
                    int addr = getAddress(variable, n);
                    push(st.get(addr));
                }
                break;

                case ADDRESS : {
                    Variable variable = getVariable((String) code.val1);
                    int n = (int) code.val2;
                    int addr = getAddress(variable, n);
                    push(addr);
                }
                break;

                case DIMVARIABLE: {
                    Variable var = getVariable((String) code.val1);
                    int n = (int) code.val2;

                    var.dimensions = n;
                    if (n == 1) {
                        var.firstDim = pop();
                    } else if (n == 2) {
                        var.secondDim = pop();
                        var.firstDim = pop();
                    }
                }
                break;

                case PLACEHOLDER : {
                    Variable variable = getVariable((String) code.val1);
                    int n = (int) code.val2;

                    if (n == 0) {
                        push(0);
                    } else if (n == 1) {
                        for (int i = 0; i < variable.firstDim; i++) {
                            push(0);
                        }
                    } else if (n == 2) {
                        for (int i = 0; i < variable.firstDim; i++) {
                            for (int j = 0; j < variable.secondDim; j++) {
                                push(0);
                            }
                        }
                    }
                }
                break;

                case EXIT : {
                    return;
                }

            }
        }
    }
}
