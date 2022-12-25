import CodeGen.LabelGen;
import CodeGen.PCode;
import CodeGen.PCodeKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

public class SyntacticalAnalyser {
    public ArrayList<Token> tokens;
    public int p;

    public Token curToken;
    
    public static final int OneStep = 1;
    public static final int TwoStep = 2;
    public boolean isCond = false;
    public static ArrayList<String> syntactic = new ArrayList<>();
    public static ArrayList<Token> forDebug = new ArrayList<>();

    public static ArrayList<Error> errors = new ArrayList<>();

    // Error Handle
    public int whileFlag = 0;
    public boolean needReturn = false;
    public HashMap<Integer, IdentifierMap> mapIdentifierGroup = new HashMap<>();
    public HashMap<String, Function> mapFunction = new HashMap<>();
    public int curScope = -1;

    // Code Gen
    public int scope = -1;
    public static ArrayList<PCode> PCodes = new ArrayList<>();
    public LabelGen labelGen = new LabelGen();
    public ArrayList<HashMap<String, String>> labelsIf = new ArrayList<>();
    public ArrayList<HashMap<String, String>> labelsWhile = new ArrayList<>();

    public void addScope() {
        scope += 1;
        curScope += 1;
        mapIdentifierGroup.put(curScope, new IdentifierMap());
    }

    public void removeScope() {
        mapIdentifierGroup.remove(curScope);
        curScope -= 1;
    }

    public void addIdentifierToScope(Token token, String kind, int kindInt) {
        mapIdentifierGroup.get(curScope).put(kind, kindInt, token, scope);
    }

    public Identifier getIdentifier(Token token) {
        Identifier identifier = null;
        for (IdentifierMap identifierMap : mapIdentifierGroup.values()) {
            if (identifierMap.has(token)) {
                identifier = identifierMap.get(token);
            }
        }
        return identifier;
    }

    public boolean identifierDefinedInScope(Token token) {
        return mapIdentifierGroup.get(curScope).has(token);
    }

    public boolean identifierDefinedGlobally(Token token) {
        for (IdentifierMap i : mapIdentifierGroup.values()) {
            if (i.has(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConst(Token t) {
        for (IdentifierMap m : mapIdentifierGroup.values()) {
            if (m.has(t)) {
                if (m.isConst(t)) {
                    return true;
                }
            }
        }

        return false;
    }
    public boolean functionDefined(Token token) {
        return mapFunction.containsKey(token.content);
    }


    public static void wrapNonterminal(Nonterminal nonterminal) {
        syntactic.add("<" + nonterminal.toString() + ">");
    }

    public SyntacticalAnalyser(ArrayList<Token> tokens) {
        this.tokens = tokens;
        p = 0;
        curToken = tokens.get(0);

        CompUnitStart();
    }

    public void handleError (int line, ErrorKind kind) {
        // Error Handle
        Error err = new Error(line, MapErr2Kind.Kind(kind));
        errors.add(err);
    }

    public void handleError() {
        System.out.println("Error at " + curToken.content + " " + curToken.line);
        RuntimeException e = new RuntimeException("handle err");
        Stream.of(e.getStackTrace()).forEach(System.out::println);

        System.exit(-1);
    }

    public Kind lookAhead(int n) {
        if (p + n >= tokens.size()) {
            return Kind.VOID;
        }
        return tokens.get(p + n).kind;
    }

    public void moveForward() {
        if (p >= tokens.size()) {
            handleError();
        }

        forDebug.add(curToken);
        syntactic.add(curToken.toString());
        p += 1;

        if (p < tokens.size()){
            curToken = tokens.get(p);
        }
    }

    public Token prevToken() {
        return tokens.get(p - 1);
    }

    public void CompUnitStart() {
        //CompUnit → {Decl} {FuncDef} MainFuncDef

        addScope();

        while (p < tokens.size()) {
            if (curToken.kind == Kind.CONSTTK) {
                DeclHandle();
            } else if (curToken.kind == Kind.INTTK) {
                if (lookAhead(OneStep) == Kind.MAINTK) {
                    MainFuncDefHandle();
                } else {
                    if (lookAhead(TwoStep) == Kind.LPARENT) {
                        FuncDefHandle();
                    } else {
                        DeclHandle();
                    }
                }
            } else if (curToken.kind == Kind.VOIDTK) {
                FuncDefHandle();
            } else {
                handleError();
            }
        }

        removeScope();

        wrapNonterminal(Nonterminal.CompUnit);
    }

    public void DeclHandle() {
        //Decl → ConstDecl | VarDecl

        if (curToken.kind == Kind.CONSTTK) {
            ConstDeclHandle();
        } else if (curToken.kind == Kind.INTTK) {
            VarDeclHandle();
        } else {
            handleError();
        }
    }

    public void ConstDeclHandle() {
        // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'

        if (curToken.kind != Kind.CONSTTK) {
            handleError();
        }
        moveForward();

        BTypeHandle();
        ConstDefHandle();
        while (curToken.kind == Kind.COMMA) {
            moveForward();
            ConstDefHandle();
        }

        if (curToken.kind != Kind.SEMICN) {
            handleError(prevToken().line, ErrorKind.MissSemiColon);
        } else {
            moveForward();
        }

        wrapNonterminal(Nonterminal.ConstDecl);
    }

    public void VarDeclHandle() {
        // VarDecl → BType VarDef { ',' VarDef } ';'

        BTypeHandle();
        VarDefHandle();

        while (curToken.kind == Kind.COMMA) {
            moveForward();
            VarDefHandle();
        }

        if (curToken.kind != Kind.SEMICN) {
            handleError(prevToken().line, ErrorKind.MissSemiColon);
        } else {
            moveForward();
        }

        wrapNonterminal(Nonterminal.VarDecl);
    }

    public void BTypeHandle() {
        // BType → 'int'

        if (curToken.kind != Kind.INTTK) {
            handleError();
        } else {
            moveForward();
        }

    }

    public void ConstDefHandle() {
        // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }

        Token identifier = curToken;

        // Error Handle
        if (identifierDefinedInScope(identifier)) {
            handleError(identifier.line, ErrorKind.Redefinition);
        }
        // Error Handle

        // CodeGen
        int kindInt = 0;
        PCodes.add(new PCode(PCodeKind.VARIABLE, scope + "-" + curToken.content));
        // CodeGen

        moveForward();

        if (curToken.kind == Kind.LBRACK) {
            // CodeGen
            kindInt += 1;
            // CodeGen

            moveForward();

            ConstExpHandle();

            // Error Handle
            if (curToken.kind != Kind.RBRACK) {
                handleError(prevToken().line, ErrorKind.MissBrack);
            } else {
                moveForward();
            }
            // Error Handle

            if (curToken.kind == Kind.LBRACK) {
                //CodeGen//
                kindInt += 1;
                //CodeGen//

                moveForward();
                ConstExpHandle();

                // Error Handle
                if (curToken.kind != Kind.RBRACK) {
                    handleError(prevToken().line, ErrorKind.MissBrack);
                } else {
                    moveForward();
                }
                // Error Handle
            }
        }

        // CodeGen
        if (kindInt > 0) {
            PCodes.add(new PCode(PCodeKind.DIMVARIABLE, scope + "-" + identifier.content, kindInt));
        }
        addIdentifierToScope(identifier, "const", kindInt);
        // CodeGen


        if (curToken.kind == Kind.ASSIGN) {
            moveForward();
            ConstInitValHandle();
        } else {
            handleError();
        }

        wrapNonterminal(Nonterminal.ConstDef);
    }

    public void VarDefHandle() {
        // VarDef → Ident { '[' ConstExp ']' }
        //          | Ident { '[' ConstExp ']' } '=' InitVal

        // CodeGen
        Token identifier = curToken;
        PCodes.add(new PCode(PCodeKind.VARIABLE, scope + "-" + identifier.content));

        int kindInt = 0;
        // CodeGe

        // Error Handle
        if (identifierDefinedInScope(identifier)) {
            handleError(curToken.line, ErrorKind.Redefinition);
        }
        // Error Handle

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }
        moveForward();

        if (curToken.kind == Kind.LBRACK) {
            // CodeGen
            kindInt += 1;
            // CodeGen

            moveForward();
            ConstExpHandle();

            if (curToken.kind != Kind.RBRACK) {
                handleError(prevToken().line, ErrorKind.MissBrack);
            } else {
                moveForward();
            }

            if (curToken.kind == Kind.LBRACK) {
                // CodeGen
                kindInt += 1;
                // CodeGen

                moveForward();
                ConstExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError(prevToken().line, ErrorKind.MissBrack);
                } else {
                    moveForward();
                }
            }
        }

        // CodeGen
        if (kindInt > 0) {
            PCodes.add(new PCode(PCodeKind.DIMVARIABLE, scope + "-" + identifier.content, kindInt));
        }
        addIdentifierToScope(identifier, "var", kindInt);
        // CodeGen

        if (curToken.kind == Kind.ASSIGN) {
            moveForward();
            InitValHandle();
        } else {
            // CodeGen
            PCodes.add(new PCode(PCodeKind.PLACEHOLDER, scope + "-" + identifier.content, kindInt));
            // Codegen
        }

        wrapNonterminal(Nonterminal.VarDef);
    }


    public void InitValHandle() {
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'

        if (curToken.kind == Kind.LBRACE) {
            moveForward();

            if (curToken.kind == Kind.RBRACE) {
                moveForward();
            } else {
                InitValHandle();

                while (curToken.kind == Kind.COMMA) {
                    moveForward();
                    InitValHandle();
                }

                if (curToken.kind != Kind.RBRACE) {
                    handleError();
                }
                moveForward();
            }
        } else {
            ExpHandle();
        }

        wrapNonterminal(Nonterminal.InitVal);
    }
    public void MainFuncDefHandle() {
        // MainFuncDef -> 'int' 'main' '(' ')' Block

        // CodeGen
        PCodes.add(new PCode(PCodeKind.MAIN, "main"));
        // CodeGen

        if (curToken.kind != Kind.INTTK) {
            handleError();
        }
        moveForward();

        if (curToken.kind != Kind.MAINTK) {
            handleError();
        }

        // Error Handle
        if (mapFunction.containsKey("main")) {
            handleError(curToken.line, ErrorKind.Redefinition);
        }
        // Error Handle

        Function function = new Function("main", "int", new ArrayList<>());
        mapFunction.put("main", function);


        moveForward();

        if (curToken.kind != Kind.LPARENT) {
            handleError();
        }
        moveForward();

        if (curToken.kind != Kind.RPARENT) {
            handleError(prevToken().line, ErrorKind.MissParenthesis);
        } else {
            moveForward();
        }

        // Error handle
        needReturn = true;
        boolean returned = BlockHandle(false);
        if (!returned) {
            handleError(prevToken().line, ErrorKind.MissReturn);
        }
        // Error Handle

        wrapNonterminal(Nonterminal.MainFuncDef);

        // CodeGen
        PCodes.add(new PCode(PCodeKind.EXIT));
        // CodeGen
    }

    public void FuncDefHandle() {
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block

        // CodeGen
        addScope();

        String rType = FuncTypeHandle();
        String funcName = curToken.content;

        PCode code = new PCode(PCodeKind.FUNC, funcName);
        Function function = new Function(funcName, rType);
        PCodes.add(code);
        // CodeGen

        // Error Handle
        if (mapFunction.containsKey(funcName)) {
            handleError(curToken.line, ErrorKind.Redefinition);
        }
        needReturn = rType.equals("int");
        // Error Handle

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }
        moveForward();

        if (curToken.kind != Kind.LPARENT) {
            handleError();
        }
        moveForward();


        if (curToken.kind != Kind.RPARENT) {
            // CodeGen
            function.params = FuncFParamsHandle();
            // CodeGen

        } else{
            // CodeGen
            function.params = new ArrayList<>();
            // CodeGen
        }

        if (curToken.kind != Kind.RPARENT) {
            handleError(prevToken().line, ErrorKind.MissParenthesis);
        } else {
            moveForward();
        }

        // CodeGen
        mapFunction.put(function.name, function);
        code.val2 = function.params.size();
        // CodeGen

        boolean returned = BlockHandle(true);

        // Error Handle
        if (needReturn && !returned){
            handleError(tokens.get(p-1).line, ErrorKind.MissReturn);
        }
        // Error Handle

        // CodeGen
        removeScope();
        PCodes.add(new PCode(PCodeKind.RETURN, 0));
        PCodes.add(new PCode(PCodeKind.FUNCEND));
        // CodeGen

        wrapNonterminal(Nonterminal.FuncDef);
    }

    public String FuncTypeHandle() {
        // FuncType -> 'void' | 'int'
        String rType;
        if (!(curToken.kind == Kind.VOIDTK || curToken.kind == Kind.INTTK)) {
            rType = "";
            handleError();
        } else if (curToken.kind == Kind.VOIDTK) {
            rType = "void";
        } else {
            rType = "int";
        }

        moveForward();

        wrapNonterminal(Nonterminal.FuncType);

        return rType;
    }

    public int FuncFParamHandle() {
        // FuncFParam -> BType Ident ['['']' {'[' ConstExp ']' }]

        BTypeHandle();

        // CodeGen
        int paramType = 0;
        Token identifier = curToken;
        // CodeGen

        // Error Handle
        if (identifierDefinedInScope(identifier)) {
            handleError(curToken.line, ErrorKind.Redefinition);
        }
        //  Error Handle

        if (curToken.kind != Kind.IDENFR) {
//            handleError();
        } else {
            moveForward();
        }

        if (curToken.kind == Kind.LBRACK) {
            // CodeGen
            paramType += 1;
            // CodeGen

            moveForward();

            if (curToken.kind != Kind.RBRACK) {
                handleError(prevToken().line, ErrorKind.MissBrack);
            } else {
                moveForward();
            }

            if (curToken.kind == Kind.LBRACK) {
                // CodeGen
                paramType += 1;
                // CodeGen

                moveForward();
                ConstExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError(prevToken().line, ErrorKind.MissBrack);
                } else {
                    moveForward();
                }
            }
        }

        wrapNonterminal(Nonterminal.FuncFParam);

        // CodeGen
        PCodes.add(new PCode(PCodeKind.PARAM, scope + "-" + identifier.content, paramType));
        addIdentifierToScope(identifier, "para", paramType);
        return paramType;
    }

    public ArrayList<Integer> FuncFParamsHandle() {
        // FuncFParams -> FuncFParam {',' FuncFParam}

        // CodeGen
        ArrayList<Integer> params = new ArrayList<>();
        params.add(FuncFParamHandle());
        // CodeGen

        while (curToken.kind == Kind.COMMA) {
            moveForward();

            // CodeGen
            params.add(FuncFParamHandle());
            // CodeGen
        }

        wrapNonterminal(Nonterminal.FuncFParams);

        return params;
    }

    public void FuncRParamsHandle(Token identifier) {
        // FuncRParams → Exp { ',' Exp }

        if (curToken.kind == Kind.SEMICN) {
            return;
        }

        //Error Handle
        ArrayList<Integer> rParams = new ArrayList<>();
        int kind = ExpHandle();
        rParams.add(kind);
        //Error Handle

        // CodeGen
        PCodes.add(new PCode(PCodeKind.REALPARAM, kind));
        // CodeGen

        while (curToken.kind == Kind.COMMA) {
            moveForward();

            // CodeGen
            kind = ExpHandle();
            rParams.add(kind);
            PCodes.add(new PCode(PCodeKind.REALPARAM, kind));
            // CodeGen
        }

        // Error Handle
        ArrayList<Integer> params = mapFunction.get(identifier.content).params;
        if (rParams.size() != params.size()) {
            handleError(identifier.line, ErrorKind.ParamCountNotMatch);
        } else {
            for (int i = 0; i < rParams.size(); i++) {
                if (!params.get(i).equals(rParams.get(i))) {
                    handleError(identifier.line, ErrorKind.ParamTypeNotMatch);
                }
            }
        }
        // Error Handle

        wrapNonterminal(Nonterminal.FuncRParams);
    }
    public boolean BlockHandle(boolean inFunc) {
        // '{' { BlockItem } '}'

        // Error Handle
        boolean returned = false;
        // Error Handle

        // CodeGen
        if (!inFunc) {
            addScope();
        }
        //Code Gen //

        if (curToken.kind != Kind.LBRACE) {
            handleError();
        }
        moveForward();

        while (curToken.kind != Kind.RBRACE) {
            returned = BlockItemHandle();
        }
        moveForward();

        // CodeGen
        if (!inFunc) {
            removeScope();
        }
        //Code Gen //

        wrapNonterminal(Nonterminal.Block);

        return returned;
    }

    public boolean BlockItemHandle() {
        // BlockItem → Decl | Stmt

        // Error Handle
        boolean returned = false;
        // Error Handle

        if (curToken.kind == Kind.CONSTTK || curToken.kind == Kind.INTTK) {
            DeclHandle();
        } else {
            returned = StmtHandle();
        }

        return returned;
    }

    public boolean StmtHandle() {
        // Stmt → LVal '=' Exp ';'
        //      | [Exp] ';' //有无Exp两种情况
        //      | Block
        //      | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
        //      | 'while' '(' Cond ')' Stmt
        //      | 'break' ';'
        //      | 'continue' ';'
        //      | 'return' [Exp] ';' // 1.有Exp 2.无Exp
        //      | LVal '=' 'getint''('')'';'
        //      | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp

        // Error Handle
        boolean returned = false;
        // Error Handle

        switch (curToken.kind) {
            case IFTK:
                // Code Gen
                HashMap<String, String> temp = new HashMap<>();
                labelsIf.add(temp);

                temp.put("if", labelGen.gen("if"));
                temp.put("else", labelGen.gen("else"));
                temp.put("if_end", labelGen.gen("if_end"));
                temp.put("if_block", labelGen.gen("if_block"));
                PCodes.add(new PCode(PCodeKind.LABEL, temp.get("if")));
                // Code Gen

                moveForward();

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                CondHandle(Kind.IFTK);

                if (curToken.kind != Kind.RPARENT) {
                    handleError(prevToken().line, ErrorKind.MissParenthesis);
                } else {
                    moveForward();
                }

                // Code Gen
                PCodes.add(new PCode(PCodeKind.JZ, temp.get("else")));
                PCodes.add(new PCode(PCodeKind.LABEL, temp.get("if_block")));
                // Code Gen

                StmtHandle();

                // Code Gen
                PCodes.add(new PCode(PCodeKind.JMP, temp.get("if_end")));
                PCodes.add(new PCode(PCodeKind.LABEL, temp.get("else")));
                // Code Gen


                if (curToken.kind == Kind.ELSETK) {
                    moveForward();
                    StmtHandle();
                }

                // Code Gen
                PCodes.add(new PCode(PCodeKind.LABEL, temp.get("if_end")));
                labelsIf.remove(labelsIf.size() - 1);
                // Code Gen

                break;

            case WHILETK:
                // Code Gen
                temp = new HashMap<>();
                labelsWhile.add(temp);

                temp.put("while", labelGen.gen("while"));
                temp.put("while_end", labelGen.gen("while_end"));
                temp.put("while_block", labelGen.gen("while_block"));
                PCodes.add(new PCode(PCodeKind.LABEL, temp.get("while")));
                // Code Gen

                moveForward();
                whileFlag += 1;

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                CondHandle(Kind.WHILETK);

                // Code Gen
                PCodes.add(new PCode(PCodeKind.JZ, temp.get("while_end")));
                PCodes.add(new PCode(PCodeKind.LABEL, temp.get("while_block")));
                // Code Gen

                if (curToken.kind != Kind.RPARENT) {
                    handleError(prevToken().line, ErrorKind.MissParenthesis);
                } else {
                    moveForward();
                }

                StmtHandle();

                // Code Gen
                PCodes.add(new PCode(PCodeKind.JMP, temp.get("while")));
                PCodes.add(new PCode(PCodeKind.LABEL, temp.get("while_end")));
                labelsWhile.remove(labelsWhile.size() - 1);
                // Code Gen

                whileFlag -= 1;

                break;

            case BREAKTK:
                // Code Gen
                PCodes.add(new PCode(PCodeKind.JMP, labelsWhile.get(labelsWhile.size() - 1).get("while_end")));
                // Code Gen

                if (whileFlag == 0) {
                    handleError(curToken.line, ErrorKind.BreakOrContinueOutsideLoop);
                }
                moveForward();

                if (curToken.kind != Kind.SEMICN) {
                    handleError(prevToken().line, ErrorKind.MissSemiColon);
                } else {
                    moveForward();
                }

                break;

            case CONTINUETK:
                // Code Gen
                PCodes.add(new PCode(PCodeKind.JMP, labelsWhile.get(labelsWhile.size() - 1).get("while")));
                // Code Gen

                if (whileFlag == 0) {
                    handleError(curToken.line, ErrorKind.BreakOrContinueOutsideLoop);
                }

                moveForward();

                if (curToken.kind != Kind.SEMICN) {
                    handleError(prevToken().line, ErrorKind.MissSemiColon);
                } else {
                    moveForward();
                }

                break;

            case RETURNTK:
                Token returnTk = curToken;
                moveForward();

                if (curToken.kind == Kind.SEMICN) {
                    moveForward();

                    // CodeGen
                    PCodes.add(new PCode(PCodeKind.RETURN, 0));
                    // CodeGen
                } else {
                    // Error Handle
                    if(!needReturn) {
                        handleError(curToken.line, ErrorKind.   ReturnInVoid);
                    }
                    // Error Handle

                    ExpHandle();

                    if (curToken.kind != Kind.SEMICN) {
                        handleError(prevToken().line, ErrorKind.MissSemiColon);
                    } else {
                        moveForward();
                    }

                    // CodeGen
                    PCodes.add(new PCode(PCodeKind.RETURN, 1));
                    // CodeGen
                }

                returned = true;
                break;

            case PRINTFTK:
                int paramCount = 0;

                Token printf = curToken;
                moveForward();

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                // CodeGen
                Token str = curToken;
                // Codegen

                if (curToken.kind != Kind.STRCON) {
                    handleError();

                }
                moveForward();

                while (curToken.kind == Kind.COMMA) {
                    moveForward();
                    ExpHandle();
                    paramCount += 1;
                }

                // Error Handle
                if (str.isIllegalFormatString()) {
                    handleError(str.line, ErrorKind.IllegalChar);
                }
                if (paramCount != str.formatParamCount()) {
                    handleError(printf.line, ErrorKind.FormatStringNotMatch);
                }

                if (curToken.kind != Kind.RPARENT) {
                    handleError(prevToken().line, ErrorKind.MissParenthesis);
                } else {
                    moveForward();
                }

                if (curToken.kind != Kind.SEMICN) {
                    handleError(prevToken().line, ErrorKind.MissSemiColon);
                } else {
                    moveForward();
                }

                // CodeGen
                PCodes.add(new PCode(PCodeKind.PRINT, str.content, paramCount));
                // CodeGen

                break;

            case LBRACE:
                BlockHandle(false);
                break;

            case IDENFR:
                boolean isLVal = false;
                for (int i = 1; lookAhead(i) != Kind.SEMICN; i++) {
                    if (lookAhead(i) == Kind.ASSIGN) {
                        isLVal = true;
                        break;
                    }
                }

                if (!isLVal) {
                    ExpHandle();
                } else {
                    // CodeGen
                    Token identifier = curToken;

                    // Error Handle
                    if (isConst(curToken)) {
                        handleError(curToken.line, ErrorKind.ConstModify);
                    }
                    // Error Handle

                    int kindInt = LValHandle();
                    PCodes.add(new PCode(PCodeKind.ADDRESS, getIdentifier(identifier).scope + "-" + identifier.content, kindInt));
                    // CodeGen

                    if (curToken.kind != Kind.ASSIGN) {
                        handleError();
                    }
                    moveForward();

                    if (curToken.kind == Kind.GETINTTK) {
                        moveForward();

                        // CodeGen
                        PCodes.add(new PCode(PCodeKind.GETINT));
                        // CodeGen

                        if (curToken.kind != Kind.LPARENT) {
                            handleError();
                        }
                        moveForward();

                        if (curToken.kind != Kind.RPARENT) {
                            handleError(prevToken().line, ErrorKind.MissParenthesis);
                        } else {
                            moveForward();
                        }
                    } else {
                        ExpHandle();
                    }

                    // CodeGen
                    PCodes.add(new PCode(PCodeKind.POP, getIdentifier(identifier).scope + "-" + identifier.content));
                }

                if (curToken.kind != Kind.SEMICN) {
                    handleError(prevToken().line, ErrorKind.MissSemiColon);
                } else {
                    moveForward();
                }

                break;

            default:
                if (curToken.kind == Kind.SEMICN) {
                    moveForward();
                } else {
                    ExpHandle();
                    if (curToken.kind != Kind.SEMICN) {
                        handleError(prevToken().line, ErrorKind.MissSemiColon);
                    } else {
                        moveForward();
                    }

                }
        }

        wrapNonterminal(Nonterminal.Stmt);

        return returned;
    }

    public int ExpHandle() {
        // Exp → AddExp

        int kind = AddExpHandle();

        wrapNonterminal(Nonterminal.Exp);

        return kind;
    }

    public int AddExpHandle() {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
        int kindInt = 0;

        kindInt = MulExpHandle();
        while (curToken.kind == Kind.PLUS || curToken.kind == Kind.MINU) {
            Kind kind = curToken.kind;

            wrapNonterminal(Nonterminal.AddExp);
            moveForward();
            kindInt = MulExpHandle();

            // CodeGen
            if (kind == Kind.PLUS) {
                PCodes.add(new PCode(PCodeKind.ADD));
            } else {
                PCodes.add(new PCode(PCodeKind.SUBTRACT));
            }
            // CodeGen
        }

        wrapNonterminal(Nonterminal.AddExp);

        return kindInt;
    }

    public int MulExpHandle() {
        // UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        int kindInt = 0;

        kindInt = UnaryExpHandle();
        while (curToken.kind == Kind.MULT || curToken.kind == Kind.DIV || curToken.kind == Kind.MOD) {
            Kind kind = curToken.kind;
            wrapNonterminal(Nonterminal.MulExp);
            moveForward();
            kindInt = UnaryExpHandle();

            // CodeGen
            if (kind == Kind.MULT) {
                PCodes.add(new PCode(PCodeKind.MULTIPLY));
            } else if (kind == Kind.DIV) {
                PCodes.add(new PCode(PCodeKind.DIVIDE));
            } else {
                PCodes.add(new PCode(PCodeKind.MOD));
            }
            //CodeGen
        }

        wrapNonterminal(Nonterminal.MulExp);

        return kindInt;
    }

    public void RelExpHandle() {
        // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp

        AddExpHandle();
        while (curToken.kind == Kind.LSS || curToken.kind == Kind.LEQ || curToken.kind == Kind.GRE || curToken.kind == Kind.GEQ) {
            // Code Gen
            PCodeKind kind;
            if (curToken.kind == Kind.LSS) {
                kind = PCodeKind.CLT;
            } else if (curToken.kind == Kind.LEQ) {
                kind = PCodeKind.CLE;
            } else if (curToken.kind == Kind.GRE) {
                kind = PCodeKind.CGT;
            } else {
                kind = PCodeKind.CGE;
            }
            // Code Gen

            wrapNonterminal(Nonterminal.RelExp);
            moveForward();
            AddExpHandle();

            // Code Gen
            PCodes.add(new PCode(kind));
            // Code Gen
        }

        wrapNonterminal(Nonterminal.RelExp);
    }

    public void EqExpHandle() {
        // RelExp | EqExp ('==' | '!=') RelExp

        RelExpHandle();
        while (curToken.kind == Kind.EQL || curToken.kind == Kind.NEQ) {

            // Code Gen
            PCodeKind kind;
            if (curToken.kind == Kind.EQL) {
                kind = PCodeKind.CEQ;
            } else {
                kind = PCodeKind.CNE;
            }
            // Code Gen

            wrapNonterminal(Nonterminal.EqExp);
            moveForward();
            RelExpHandle();

            // Code Gen
            PCodes.add(new PCode(kind));
            // Code Gen

        }

        wrapNonterminal(Nonterminal.EqExp);
    }

    public void LAndExpHandle(Kind from, String label) {
        // EqExp | LAndExp '&&' EqExp

        EqExpHandle();
        if (curToken.kind == Kind.AND) {
            PCodes.add(new PCode(PCodeKind.JZ, label));
        }

        int count = 0;
        while (curToken.kind == Kind.AND) {
            count += 1;
            wrapNonterminal(Nonterminal.LAndExp);
            moveForward();
            EqExpHandle();

            // Code Gen
            PCodes.add(new PCode(PCodeKind.AND));

            if (curToken.kind == Kind.AND){
                PCodes.add(new PCode(PCodeKind.JZ, label));
            }
            // Code Gen
        }

        wrapNonterminal(Nonterminal.LAndExp);
    }

    public void LOrExpHandle(Kind from) {
        // LOrExp → LAndExp | LOrExp '||' LAndExp

        // Code Gen
        String label = labelGen.gen("cond_" + 0);
        LAndExpHandle(from, label);
        PCodes.add(new PCode(PCodeKind.LABEL, label));
        if (curToken.kind == Kind.OR) {
            if (from == Kind.IFTK) {
                PCodes.add(new PCode(PCodeKind.JNZ, labelsIf.get(labelsIf.size() - 1).get("if_block")));
            } else {
                PCodes.add(new PCode(PCodeKind.JNZ, labelsWhile.get(labelsWhile.size() - 1).get("while_block")));
            }
        }
        // Code Gen

        int count = 0;
        while (curToken.kind == Kind.OR) {
            count += 1;

            // Code Gen
            label = labelGen.gen("cond_" + count);
            // Code Gen

            wrapNonterminal(Nonterminal.LOrExp);
            moveForward();
            LAndExpHandle(from, label);

            // Code Gen
            PCodes.add(new PCode(PCodeKind.LABEL, label));
            PCodes.add(new PCode(PCodeKind.OR));

            if (curToken.kind == Kind.OR){
                if (from == Kind.IFTK) {
                    PCodes.add(new PCode(PCodeKind.JNZ, labelsIf.get(labelsIf.size() - 1).get("if_block")));
                } else {
                    PCodes.add(new PCode(PCodeKind.JNZ, labelsWhile.get(labelsIf.size() - 1).get("while_block")));
                }
            }
            // Code Gen
        }

        wrapNonterminal(Nonterminal.LOrExp);
    }

    public void CondHandle(Kind kind) {
        isCond = true;
        LOrExpHandle(kind);
        isCond = false;

        wrapNonterminal(Nonterminal.Cond);
    }

    public int UnaryExpHandle() {
        // UnaryExp → PrimaryExp
        //          | Ident '(' [FuncRParams] ')'
        //          | UnaryOp UnaryExp

        int kindInt = 0;

        if (curToken.kind == Kind.IDENFR) {
            if (lookAhead(OneStep) == Kind.LPARENT) {
                Token identifier = curToken;

                // Error Handle
                ArrayList<Integer> params = null;
                if (!functionDefined(identifier)) {
                    handleError(identifier.line, ErrorKind.Undefined);
                } else {
                    params = mapFunction.get(identifier.content).params;
                }
                // Error Handle

                moveForward();

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                if (curToken.kind == Kind.RPARENT) {
                    if (params != null && params.size() != 0) {
                        handleError(identifier.line, ErrorKind.ParamCountNotMatch);
                    }
                    moveForward();
                } else {
                    FuncRParamsHandle(identifier);
                    if (curToken.kind != Kind.RPARENT) {
                        handleError(prevToken().line, ErrorKind.MissParenthesis);
                    } else {
                        moveForward();
                    }
                }

                // CodeGen
                PCodes.add(new PCode(PCodeKind.CALL, identifier.content));
                if (mapFunction.containsKey(identifier.content) && mapFunction.get(identifier.content).rType.equals("void")) {
                    kindInt = -1;
                }
                // CodeGen

            } else {
                kindInt = PrimaryExpHandle();
            }

        } else if (curToken.kind == Kind.LPARENT || curToken.kind == Kind.INTCON) {
            kindInt = PrimaryExpHandle();
        } else {
            Kind kind = curToken.kind;
            UnaryOpHandle();
            UnaryExpHandle();

            // CodeGen
            if (kind == Kind.PLUS) {
                PCodes.add(new PCode(PCodeKind.POS));
            } else if (kind == Kind.MINU) {
                PCodes.add(new PCode(PCodeKind.NEG));
            } else {
                PCodes.add(new PCode(PCodeKind.NOT));
            }
            // CodeGen

        }

        wrapNonterminal(Nonterminal.UnaryExp);

        return kindInt;
    }

    public void UnaryOpHandle() {
        // UnaryOp → '+' | '−' | '!'
        // '!' Only in CondExp

        if (curToken.kind == Kind.PLUS || curToken.kind == Kind.MINU) {
            moveForward();
        } else if (curToken.kind == Kind.NOT && isCond){
            moveForward();
        } else {
            handleError();
        }

        wrapNonterminal(Nonterminal.UnaryOp);
    }

    public int PrimaryExpHandle() {
        // PrimaryExp -> '(' Exp ')'
        //              | LVal
        //              | Number
        int kindInt = 0;
        if (curToken.kind == Kind.LPARENT) {
            moveForward();
            ExpHandle();

            if (curToken.kind != Kind.RPARENT) {
                handleError();
            }
            moveForward();
        } else if (curToken.kind == Kind.INTCON) {
            NumberHandle();
        } else if (curToken.kind == Kind.IDENFR) {

            // CodeGen
            Token identifier = curToken;
            kindInt = LValHandle();
            if (kindInt == 0) {
                PCodes.add(new PCode(PCodeKind.VALUE, getIdentifier(identifier).scope + "-" + identifier.content, kindInt));
            } else {
                PCodes.add(new PCode(PCodeKind.ADDRESS, getIdentifier(identifier).scope + "-" + identifier.content, kindInt));
            }
            // CodeGen

        } else {
            handleError();
        }

        wrapNonterminal(Nonterminal.PrimaryExp);

        return kindInt;
    }

    public void NumberHandle() {
        // CodeGen
        PCodes.add(new PCode(PCodeKind.PUSH, Integer.parseInt(curToken.content)));
        // CodeGen

        if (curToken.kind != Kind.INTCON) {
            handleError();
        }
        moveForward();

        wrapNonterminal(Nonterminal.Number);
    }

    public int LValHandle() {
        // LVal -> Ident {'[' Exp ']'}

        // CodeGen
        int kindInt = 0;
        Token identifier = curToken;
        PCodes.add(new PCode(PCodeKind.PUSH, getIdentifier(identifier).scope + "-" + identifier.content));
        // CodeGen

        // Error Handle
        if (!identifierDefinedGlobally(identifier)) {
            handleError(identifier.line, ErrorKind.Undefined);
        }

        if (curToken.kind == Kind.IDENFR) {
            moveForward();

            if (curToken.kind == Kind.LBRACK) {
                kindInt += 1;
                moveForward();
                ExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError(prevToken().line, ErrorKind.MissBrack);
                } else {
                    moveForward();
                }
                if (curToken.kind == Kind.LBRACK) {
                    kindInt += 1;
                    moveForward();
                    ExpHandle();

                    if (curToken.kind != Kind.RBRACK) {
                        handleError(prevToken().line, ErrorKind.MissBrack);
                    } else {
                        moveForward();
                    }
                }
            }
        }

        wrapNonterminal(Nonterminal.LVal);

        if (identifierDefinedGlobally(identifier)) {
            return getIdentifier(identifier).intType - kindInt;
        }
        return 0;
    }

    public void ConstExpHandle() {
        // ConstExp → AddExp

        AddExpHandle();

        wrapNonterminal(Nonterminal.ConstExp);
    }

    public void ConstInitValHandle() {
        // ConstInitVal → ConstExp
        //             | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'

        if (curToken.kind == Kind.LBRACE) {
            moveForward();

            if (curToken.kind == Kind.RBRACE) {
                moveForward();
            } else {
                ConstInitValHandle();
                while (curToken.kind == Kind.COMMA) {
                    moveForward();
                    ConstInitValHandle();
                }

                if (curToken.kind != Kind.RBRACE) {
                    handleError();
                }
                moveForward();
            }
        } else {
            ConstExpHandle();
        }

        wrapNonterminal(Nonterminal.ConstInitVal);
    }
}
