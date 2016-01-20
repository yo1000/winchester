package com.yo1000.winchester.analyzer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yoichi.kikuchi on 2016/01/15.
 */
public class ReferencedDependency implements Analyzer {
    protected static final Pattern CLASS_NAME_PATTERN = Pattern.compile("[()@ ]L([a-zA-Z0-9_\\$/]+);");

    @Override
    public Map<String, Integer> analyze(Path directoryPath, List<String> extensions) throws IOException {
        Map<String, Integer> dependencies = new TreeMap<>();

        Files.list(directoryPath).forEach(path -> {
            File file = path.toFile();

            if (file.isFile() && extensions.stream().anyMatch(ext -> file.getName().toLowerCase().endsWith(ext))) {
                try (InputStream stream = new FileInputStream(file)) {
                    this.analyze(stream).forEach((key, val) -> {
                        Integer count = dependencies.get(key);
                        dependencies.put(key, count != null ? count + val : val);
                    });
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }

            if (file.isDirectory()) {
                try {
                    this.analyze(path, extensions).forEach((key, val) -> {
                        Integer count = dependencies.get(key);
                        dependencies.put(key, count != null ? count + val : val);
                    });
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }
        });

        return dependencies;
    }

    protected Map<String, Integer> analyze(InputStream stream) throws IOException {
        ClassReader reader = new ClassReader(stream);
        try (StringWriter writer = new StringWriter()) {
            TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(writer));
            reader.accept(visitor, ClassReader.SKIP_FRAMES);

            String trace = writer.toString();
            Matcher matcher = CLASS_NAME_PATTERN.matcher(trace);

            Map<String, Integer> dependencies = new TreeMap<>();

            while (matcher.find()) {
                String className = matcher.group(1);
                int count = dependencies.containsKey(className) ? dependencies.get(className) : 0;
                dependencies.put(className, count + 1);
            }

            return dependencies;
        }
    }
}
