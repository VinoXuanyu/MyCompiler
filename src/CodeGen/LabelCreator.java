package CodeGen;

public class LabelCreator {
    public int num = 0;

    public String getLabel(String kind) {
        num++;
        return "label_" + kind + "_"+ num;
    }
}
