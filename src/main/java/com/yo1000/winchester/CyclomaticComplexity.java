package com.yo1000.winchester;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by yoichi.kikuchi on 2016/01/14.
 */
public class CyclomaticComplexity {
    protected static final List<String> COMPLEXITY_TOKENS = Arrays.asList(
            "if", "else", "switch", "case", "default", "for", "while","do", "continue", "break",
            "catch", "finally", "throw", "throws", "return", "&&", "||", "?", ":"
    );

    protected static final List<String> DEFAULT_EXTENSIONS = Arrays.asList(
            ".java", ".groovy"
    );

    public static void main(String[] args) throws IOException {
        String directoryPath = args.length >= 1 ? "file:" + args[0] : null;
        String filterPackage = args.length >= 2 ? args[1] : ".*";

        new CyclomaticComplexity().analyze(Paths.get(URI.create(directoryPath)), DEFAULT_EXTENSIONS).entrySet().stream()
                .filter(entry -> entry.getKey().matches(filterPackage))
                .sorted((x, y) -> (int) (y.getValue() - x.getValue()))
                .forEach(entry -> System.out.printf("%s %d\n", entry.getKey(), entry.getValue()));
    }

    public Map<String, Long> analyze(Path directory, List<String> extensions) throws IOException {
        Map<String, Long> dependencies = new TreeMap<>();

        Files.list(directory).forEach(path -> {
            File file = path.toFile();

            if (file.isFile() && extensions.stream().anyMatch(ext -> file.getName().toLowerCase().endsWith(ext))) {
                try {
                    dependencies.put(path.toString(), this.analyze(new FileInputStream(path.toFile())));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }

            if (file.isDirectory()) {
                try {
                    dependencies.putAll(this.analyze(path, extensions));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }
        });

        return dependencies;
    }

    public long analyze(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines()
                    .map(line -> Collections.list(new StringTokenizer(line)).stream()
                        .filter(token -> COMPLEXITY_TOKENS.contains(token))
                        .count())
                    .reduce((totalTokens, lineTokens) -> totalTokens + lineTokens)
                    .get();
        }
    }
}
