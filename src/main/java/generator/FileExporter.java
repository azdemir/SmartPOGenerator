package generator;

import java.nio.file.Files;
import java.nio.file.Paths;

public class FileExporter {

    public static void save(String path, String code) throws Exception {

        Files.write(Paths.get(path), code.getBytes());

    }

}