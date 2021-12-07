import java.lang.invoke.MethodType;
import java.util.concurrent.Callable;
import java.nio.file.Path;
import java.io.File;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.util.concurrent.Callable;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.ResourceScope;

// RUN
//   LANG=C java --add-modules jdk.incubator.foreign --enable-native-access=ALL-UNNAMED ./Main.java
public class Main implements Consumer<String> {
    static <R> R must(Callable<R> func) {
        try {
            return func.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String...args) throws Exception {
        new Main().accept("target/debug/libv1.so");
        // same class loader.
        // Exception in thread "main" java.lang.UnsatisfiedLinkError: Native Library <...>/target/debug/libv2.so already loaded in another classloader
        new Main().accept("target/debug/libv2.so");

        // create new classloader. (same class path)
        var cp = System.getProperty("java.class.path");
        var paths = Stream.of(cp.split(Pattern.quote(File.pathSeparator))).map(p -> must(() -> Path.of(p).toUri().toURL())).toArray(URL[]::new);
        var loader = new URLClassLoader(paths, null);
        @SuppressWarnings("unchecked")
        var instance = (Consumer<String>) loader.loadClass("Main").getDeclaredConstructor().newInstance();
        instance.accept("target/debug/libv2.so");
        instance.accept("target/debug/libv2.so");
    }

    @Override
    public void accept(String lib) {
        System.load(Path.of(lib).toAbsolutePath().toString());

        var hello = CLinker.getInstance().downcallHandle(
            SymbolLookup.loaderLookup().lookup("hello").orElseThrow(),
            MethodType.methodType(void.class),
            FunctionDescriptor.ofVoid());
        try {
            hello.invokeExact();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
