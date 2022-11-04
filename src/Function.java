import java.util.ArrayList;

public class Function {
    public String name;
    public String rType;
    public ArrayList<Integer> params;

    public Function(String name, String rType) {
        this.name = name;
        this.rType = rType;
    }

    public Function(String name, String rType, ArrayList<Integer> params) {
        this.name = name;
        this.rType = rType;
        this.params = params;
    }
}
