package com.yo1000.winchester.analyzer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by yoichi.kikuchi on 2016/02/02.
 */
public class SyntheticComplexity extends CyclomaticComplexity {
    protected static final Pattern CLASS_NAME_PATTERN = Pattern.compile("[()@ ]L([a-zA-Z0-9_\\$/]+);");

    private Pattern packagePattern;
    private String classExtension;

    public SyntheticComplexity(String packagePattern) {
        this(packagePattern, ".class");
    }

    public SyntheticComplexity(String packagePattern, String classExtension) {
        this.packagePattern = Pattern.compile(packagePattern);
        this.classExtension = classExtension;
    }

    @Override
    public Map<String, Integer> analyze(Path directory, List<String> extensions) throws IOException {
        Map<String, Integer> complexities = super.analyze(directory, extensions).entrySet().parallelStream()
                .map(entry -> {
                    Matcher matcher = this.getPackagePattern().matcher(entry.getKey());
                    return new AbstractMap.SimpleEntry<>(
                            matcher.find() ? matcher.group(1).replaceAll("\\..+", "") : "",
                            entry.getValue());
                })
                .filter(entry -> !entry.getKey().isEmpty() && entry.getValue() != null)
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(),
                        (i1, i2) -> i1 > i2 ? i1 : i2));

        Map<String, Set<String>> relations = this.analyze(directory).entrySet().parallelStream()
                .map(entry -> {
                    Matcher matcher = this.getPackagePattern().matcher(entry.getKey());
                    String className = matcher.find() ? matcher.group(1).replaceAll("\\..+", "") : "";
                    entry.getValue().remove(className);

                    return new AbstractMap.SimpleEntry<>(className, entry.getValue());
                })
                .filter(entry -> !entry.getKey().isEmpty())
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(), (ss1, ss2) -> {
                    ss1.addAll(ss2);
                    return ss1;
                }));

        List<RelationalClass> relationalClasses = complexities.keySet().parallelStream()
                .map(s -> this.transform(complexities, relations, s, null))
                .collect(Collectors.toList());

        return relationalClasses.parallelStream()
                .map(relationalClass -> new AbstractMap.SimpleEntry<>(relationalClass.getClassName(), relationalClass.sum()))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    protected RelationalClass transform(Map<String, Integer> complexities, Map<String, Set<String>> relations,
                                        String targetClassName, Set<String> histories) {
        Set<String> classNameHistories = histories != null ? histories : new HashSet<>();

        RelationalClass relationalClass = new RelationalClass(targetClassName);
        relationalClass.setComplexity(complexities.containsKey(targetClassName) ? complexities.get(targetClassName) : 0);

        if (!relations.containsKey(targetClassName)) {
            return relationalClass;
        }

        for (String relationalClassName : relations.get(targetClassName)) {
            if (classNameHistories.contains(relationalClassName)) {
                continue;
            }

            classNameHistories.add(relationalClassName);
            relationalClass.getRelationalClassSet().add(this.transform(
                    complexities, relations, relationalClassName, classNameHistories));
        }

        return relationalClass;
    }

    protected Map<String, Set<String>> analyze(Path directory) throws IOException {
        Map<String, Set<String>> relations = new HashMap<>();

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                File f = file.toFile();

                if (!f.getName().toLowerCase().endsWith(SyntheticComplexity.this.getClassExtension())) {
                    return super.visitFile(file, attrs);
                }

                try (InputStream stream = new FileInputStream(f)) {
                    ClassReader reader = new ClassReader(stream);

                    try (StringWriter writer = new StringWriter()) {
                        TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(writer));
                        reader.accept(visitor, ClassReader.SKIP_FRAMES);

                        String trace = writer.toString();
                        Matcher classMatcher = CLASS_NAME_PATTERN.matcher(trace);
                        Set<String> classNames = new HashSet<>();

                        while (classMatcher.find()) {
                            String className = "/" + classMatcher.group(1);
                            Matcher packageMatcher = SyntheticComplexity.this.getPackagePattern().matcher(className);

                            if (packageMatcher.find()) {
                                classNames.add(packageMatcher.group(1).replaceAll("\\..+", ""));
                            }
                        }

                        relations.put(f.getAbsolutePath().replace('\\', '/'), classNames);
                        return super.visitFile(file, attrs);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });

        return relations;
    }

    public Pattern getPackagePattern() {
        return packagePattern;
    }

    protected void setPackagePattern(Pattern packagePattern) {
        this.packagePattern = packagePattern;
    }

    public String getClassExtension() {
        return classExtension;
    }

    protected void setClassExtension(String classExtension) {
        this.classExtension = classExtension;
    }

    protected static class RelationalClass {
        private String className;
        private int complexity;
        private Set<RelationalClass> relationalClassSet;

        public RelationalClass(String className) {
            this(className, 0, new HashSet<>());
        }

        public RelationalClass(String className, int complexity, Set<RelationalClass> relationalClassSet) {
            this.className = className;
            this.complexity = complexity;
            this.relationalClassSet = relationalClassSet;
        }

        public String getClassName() {
            return className;
        }

        protected void setClassName(String className) {
            this.className = className;
        }

        public int getComplexity() {
            return complexity;
        }

        public void setComplexity(int complexity) {
            this.complexity = complexity;
        }

        public Set<RelationalClass> getRelationalClassSet() {
            return relationalClassSet;
        }

        protected void setRelationalClassSet(Set<RelationalClass> relationalClassSet) {
            this.relationalClassSet = relationalClassSet;
        }

        public int sum() {
            return this.sum(this) - this.getComplexity();
        }

        protected int sum(RelationalClass targetClass) {
            int sum = targetClass.getComplexity();

            for (RelationalClass relationalClass : targetClass.getRelationalClassSet()) {
                sum += this.sum(relationalClass);
            }

            return sum;
        }
    }
}
