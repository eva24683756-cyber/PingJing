import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.io.File;
import java.util.*;

/** Syntax-only parse: does not pretend to compile Android sources without Android SDK. */
public class ParseJavaSources {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            List<File> sources = new ArrayList<>();
            for (String s : args) sources.add(new File(s));
            Iterable<? extends JavaFileObject> units = files.getJavaFileObjectsFromFiles(sources);
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none"), null, units);
            int parsed = 0;
            for (Object ignored : task.parse()) parsed++;
            if (!diagnostics.getDiagnostics().isEmpty()) {
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    System.err.println(d.getKind() + ": " + d.getMessage(Locale.ROOT));
                }
                throw new IllegalStateException("Java syntax diagnostics found");
            }
            System.out.println("PASS: syntax parsed " + parsed + " Java source files");
        }
    }
}
