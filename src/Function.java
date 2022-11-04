import java.util.ArrayList;

public class Function {
    public String content;
    public String rType;
    public ArrayList<Integer> params;

    public Function(Token token, String rType) {
        this.content = token.content;
        this.rType = rType;
    }
}
