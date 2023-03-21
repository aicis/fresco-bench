package dk.alexandra.benchmarking;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Util {
    public static String parseTimes(String name, List<Long> times) {
        return name + ", " + mean(times) + ", " + std(times) + "\n";
    }

    public static double mean(List<Long> times) {
        int iterations = times.size();
        return ((double) times.stream().mapToInt(Long::intValue).sum())/iterations;
    }

    public static double std(List<Long> times ) {
        double mean = mean(times);
        double temp = 0.0;
        for (long current : times) {
            temp += (((double)current) - mean)*(((double)current) - mean);
        }
        int iterations = times.size();
        return Math.sqrt(temp/((double)(iterations - 1)));
    }


    public static void writeResults(String directory, List<String> toWrite) throws IOException {
        Path path = Paths.get(directory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        Path filePath = Paths.get(directory + "/benchmark.csv");
        BufferedWriter buffer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
        for (String currentLine : toWrite) {
            buffer.write(currentLine + "\n");
        }
        buffer.close();
    }
}
