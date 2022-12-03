import CodeGen.PCode;

import java.io.*;
import java.util.ArrayList;

public class FileHandler {
    public static FileReader reader;
    public static FileWriter writer;
    public static FileWriter testWriter;
    public static String codes;

    public FileHandler() throws IOException {
        reader = new FileReader(Compiler.input);
        codes = parseInputFile();
        writer = new FileWriter(Compiler.output);

        if (!Compiler.testOutput.isEmpty()) {
            testWriter = new FileWriter(Compiler.testOutput);
        }
    }

    public String parseInputFile() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuffer stringBuffer = new StringBuffer();
        String s;
        while ((s = bufferedReader.readLine()) != null) {
            stringBuffer.append(s).append("\n");
        }
        return stringBuffer.toString();
    }

    public void printLines(ArrayList<String> lines) throws IOException {
        for (String str : lines) {
            writer.write(str + "\n");
        }
        writer.flush();
        writer.close();
    }

    public void printPCodes(ArrayList<PCode> lines) throws IOException {
        for (PCode code : lines) {
            testWriter.write(code.toString() + "\n");
        }
        testWriter.flush();
        testWriter.close();
    }

    public void printErrors(ArrayList<Error> lines) throws IOException {
        for (Error err : lines) {
            System.out.println(err.toString());
            testWriter.write(err.toString() + "\n");
        }
        testWriter.flush();
        testWriter.close();
    }

    public void printRaw(ArrayList<String> lines) throws IOException {
        for (String str : lines) {
            writer.write(str);
        }

        writer.flush();
        writer.close();
    }
}
