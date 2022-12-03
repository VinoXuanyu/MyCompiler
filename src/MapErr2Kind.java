import java.util.HashMap;

public class MapErr2Kind {
    public static HashMap<ErrorKind, String> map = new HashMap<>();

    static {
        map.put(ErrorKind.IllegalChar, "a");
        map.put(ErrorKind.Redefinition, "b");
        map.put(ErrorKind.Undefined, "c");
        map.put(ErrorKind.ParamCountNotMatch, "d");
        map.put(ErrorKind.ParamTypeNotMatch, "e");
        map.put(ErrorKind.ReturnInVoid, "f");
        map.put(ErrorKind.MissReturn, "g");
        map.put(ErrorKind.ConstModify, "h");
        map.put(ErrorKind.MissSemiColon, "i");
        map.put(ErrorKind.MissParenthesis, "j");
        map.put(ErrorKind.MissBrack, "k");
        map.put(ErrorKind.FormatStringNotMatch, "l");
        map.put(ErrorKind.BreakOrContinueOutsideLoop, "m");
    }

    public static String Kind(ErrorKind kind) {
        return map.get(kind);
    }

}
