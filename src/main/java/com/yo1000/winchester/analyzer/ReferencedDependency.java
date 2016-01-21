package com.yo1000.winchester.analyzer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
    public Map<String, Integer> analyze(Path directory, List<String> extensions) throws IOException {
        Map<String, Integer> dependencies = new TreeMap<>();

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                File f = file.toFile();

                if (extensions.stream().anyMatch(ext -> f.getName().toLowerCase().endsWith(ext))) {
                    try (InputStream stream = new FileInputStream(f)) {
                        ReferencedDependency.this.analyze(stream).forEach((key, val) -> {
                            Integer count = dependencies.get(key);
                            dependencies.put(key, count != null ? count + val : val);
                        });
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                return super.visitFile(file, attrs);
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
