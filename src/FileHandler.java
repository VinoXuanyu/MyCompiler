import java.io.*;
import java.util.ArrayList;

public class FileHandler {
    public static FileReader reader;
    public static FileWriter writer;
    public static String codes;

    public FileHandler() throws IOException {
        reader = new FileReader(Compiler.input);
        codes = ParseInputFile();
        writer = new FileWriter(Compiler.output);
    }

    public String ParseInputFile() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuffer stringBuffer = new StringBuffer();
        String s;
        while ((s = bufferedReader.readLine()) != null) {
            stringBuffer.append(s).append("\n");
        }
        return stringBuffer.toString();
    }

    public void PrintLines(ArrayList<String> lines) throws IOException {
        for (String str : lines) {
            writer.write(str + "\n");
        }
        writer.flush();
        writer.close();
    }
}
