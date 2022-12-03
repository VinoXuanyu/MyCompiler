public class Error {
    public int line;
    public String kind;

    public Error(int line, String kind) {
        this.line = line;
        this.kind = kind;
    }

    @Override
    public String toString() {
        return line + " " + kind;
    }
}
