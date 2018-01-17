package com.castlabs.dash.dashfragmenter.cmdlines.trackoptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by sannies on 25.07.17.
 */
public class Glob {
    static public List<File> get(File base, String pattern) {
        Path p = Paths.get(pattern);
        List<Path> ff;
        List<String> segments = new ArrayList<>(Arrays.asList(pattern.split(Pattern.quote(File.separator))));
        if ( p.isAbsolute() ) {
            Path absStart = Paths.get(segments.remove(0) + File.separator);
            assert Files.isDirectory(absStart);
            ff = Collections.singletonList(absStart);
        } else {
            assert base.isDirectory();
            ff = Collections.singletonList(base.getAbsoluteFile().toPath());
        }
        while (!segments.isEmpty()) {
            String segment = segments.remove(0);
            ArrayList<Path> fff = new ArrayList<>();

            for (Path d : ff) {
                PathMatcher matcher = FileSystems.getDefault()
                        .getPathMatcher("glob:" + segment);
                if (Files.isDirectory(d)) {
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(d)) {
                        for (Path path : directoryStream) {
                            if (matcher.matches(path.getFileName())) {
                                fff.add(path);
                            }
                        }
                    } catch (IOException ex) {}

                }
            }
            ff = fff;
        }
        List<File> files = new ArrayList<>();
        for (Path path : ff) {
            files.add(path.toFile());
        }
        return files;
    }

    public static void main(String[] args) {
        System.err.println(Glob.get(new File(""), "/Users/sannies/dev/dashencrypt/*/checkout/*.xml"));
    }
}
