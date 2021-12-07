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

public class Main2 implements Consumer<String> {
    static <R> R must(Callable<R> func) {
        try {
            return func.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String...args) throws Exception {
        new Main2().accept("target/debug/libv1.so");
        // same class loader.
        new Main2().accept("target/debug/libv2.so");

        // create new classloader. (same class path)
        var cp = System.getProperty("java.class.path");
        var paths = Stream.of(cp.split(Pattern.quote(File.pathSeparator))).map(p -> must(() -> Path.of(p).toUri().toURL())).toArray(URL[]::new);
        var loader = new URLClassLoader(paths, null);
        @SuppressWarnings("unchecked")
        var instance = (Consumer<String>) loader.loadClass("Main2").getDeclaredConstructor().newInstance();
        instance.accept("target/debug/libv1.so");
        instance.accept("target/debug/libv2.so");
    }

    @Override
    public void accept(String lib) {
        try {
            var handle = dlopen(lib, 0x00001 /* RTLD_LAZY */);
            var helloAddr = dlsym(handle, "hello");
            var hello = CLinker.getInstance().downcallHandle(
                helloAddr,
                MethodType.methodType(void.class),
                FunctionDescriptor.ofVoid());
            hello.invokeExact();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            //dlclose(handle);
        }
    }

    static MemoryAddress dlopen(String filename, int flag) throws Throwable {
        var dlopen = CLinker.getInstance().downcallHandle(
                CLinker.systemLookup().lookup("dlopen").orElseThrow(),
                MethodType.methodType(MemoryAddress.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(CLinker.C_POINTER, CLinker.C_POINTER, CLinker.C_INT));
        try (var scope = ResourceScope.newConfinedScope()) {
            var cname = CLinker.toCString(filename, scope);
            var result = (MemoryAddress) dlopen.invokeExact(cname.address(), flag);
            if (result.equals(MemoryAddress.NULL)) {
                String msg = dlerror();
                throw new RuntimeException(msg);
            }
            return result;
        }
    }

    static String dlerror() {
        try {
            var dlerror = CLinker.getInstance().downcallHandle(
                    CLinker.systemLookup().lookup("dlerror").orElseThrow(),
                    MethodType.methodType(MemoryAddress.class),
                    FunctionDescriptor.of(CLinker.C_POINTER));
            var result = (MemoryAddress) dlerror.invokeExact();
            if (result.equals(MemoryAddress.NULL)) {
                return "";
            }
            return CLinker.toJavaString(result);
        } catch (Throwable t) {
            return "failed to get error message. because: " + t;
        }
    }

    static MemoryAddress dlsym(MemoryAddress handle, String symbol) throws Throwable {
        var dlsym = CLinker.getInstance().downcallHandle(
                CLinker.systemLookup().lookup("dlsym").orElseThrow(),
                MethodType.methodType(MemoryAddress.class, MemoryAddress.class, MemoryAddress.class),
                FunctionDescriptor.of(CLinker.C_POINTER, CLinker.C_POINTER, CLinker.C_POINTER));
        try (var scope = ResourceScope.newConfinedScope()) {
            var cname = CLinker.toCString(symbol, scope);
            var result = (MemoryAddress) dlsym.invokeExact(handle, cname.address());
            if (result.equals(MemoryAddress.NULL)) {
                String msg = dlerror();
                throw new RuntimeException(msg);
            }
            return result;
        }
    }

    static void dlclose(MemoryAddress handle) throws Throwable {
        var dlclose = CLinker.getInstance().downcallHandle(
                CLinker.systemLookup().lookup("dlclose").orElseThrow(),
                MethodType.methodType(int.class, MemoryAddress.class),
                FunctionDescriptor.of(CLinker.C_INT, CLinker.C_POINTER));
        var result = (int) dlclose.invokeExact(handle); // Probably no NPE will occur. (Object -> Integer -> int)
        if (result != 0) {
            String msg = dlerror();
            throw new RuntimeException(msg);
        }
    }

}
