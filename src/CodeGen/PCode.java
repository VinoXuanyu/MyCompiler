package CodeGen;

public class PCode {
    private PCodeKind kind;
    private Object val1 = null;
    private Object val2 = null;

    public PCode(PCodeKind kind) {
        this.kind = kind;
    }

    public PCode(PCodeKind kind, Object val1) {
        this.kind = kind;
        this.val1 = val1;
    }

    public PCode(PCodeKind kind, Object val1, Object val2) {
        this.kind = kind;
        this.val1 = val1;
        this.val2 = val2;
    }

    @Override
    public String toString() {
        if (kind.equals(PCodeKind.LABEL)) {
            return val1.toString() + ": ";
        }
        if (kind.equals(PCodeKind.FUNC)) {
            return "FUNC @" + val1.toString() + ":";
        }
        if (kind.equals(PCodeKind.CALL)) {
            return "$" + val1.toString();
        }
        if (kind.equals(PCodeKind.PRINT)) {
            return kind + " " + val1;
        }
        String a = val1 != null ? val1.toString() : "";
        String b = val2 != null ? ", " + val2.toString() : "";
        return kind + " " + a + b;
    }
}
