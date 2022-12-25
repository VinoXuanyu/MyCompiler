package CodeGen;

public class LabelGen {
    public int num = 0;

    public String gen(String kind) {
        num++;
        return "label_" + kind + "_"+ num;
    }
}
