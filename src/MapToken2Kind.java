import java.util.HashMap;

public class MapToken2Kind {
    public static HashMap<String, Kind> map = new HashMap<>();

    static {
        map.put("main", Kind.MAINTK);
        map.put("const", Kind.CONSTTK);
        map.put("int", Kind.INTTK);
        map.put("break", Kind.BREAKTK);
        map.put("continue", Kind.CONTINUETK);
        map.put("if", Kind.IFTK);
        map.put("else", Kind.ELSETK);
        map.put("!", Kind.NOT);
        map.put("&&", Kind.AND);
        map.put("||", Kind.OR);
        map.put("while", Kind.WHILETK);
        map.put("do", Kind.DOTK);
        map.put("getint", Kind.GETINTTK);
        map.put("printf", Kind.PRINTFTK);
        map.put("return", Kind.RETURNTK);
        map.put("+", Kind.PLUS);
        map.put("-", Kind.MINU);
        map.put("void", Kind.VOIDTK);
        map.put("*", Kind.MULT);
        map.put("/", Kind.DIV);
        map.put("%", Kind.MOD);
        map.put("<", Kind.LSS);
        map.put("<=", Kind.LEQ);
        map.put(">", Kind.GRE);
        map.put(">=", Kind.GEQ);
        map.put("==", Kind.EQL);
        map.put("!=", Kind.NEQ);
        map.put("=", Kind.ASSIGN);
        map.put(";", Kind.SEMICN);
        map.put(",", Kind.COMMA);
        map.put("(", Kind.LPARENT);
        map.put(")", Kind.RPARENT);
        map.put("[", Kind.LBRACK);
        map.put("]", Kind.RBRACK);
        map.put("{", Kind.LBRACE);
        map.put("}", Kind.RBRACE);
    }

    public static Kind kind(String identifier) {
        return map.get(identifier);
    }

}
