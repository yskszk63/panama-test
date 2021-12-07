import java.lang.invoke.MethodType;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.ResourceScope;

// RUN
//   LANG=C java --add-modules jdk.incubator.foreign --enable-native-access=ALL-UNNAMED ./Main3.java
public class Main3 {
    public static void main(String...args) throws Throwable {
        var puts = CLinker.getInstance().downcallHandle(
            CLinker.systemLookup().lookup("puts").orElseThrow(),
            MethodType.methodType(void.class, MemoryAddress.class),
            FunctionDescriptor.ofVoid(CLinker.C_POINTER));
        try (var scope = ResourceScope.newConfinedScope()) {
            var cstr = CLinker.toCString("Hello, World!", scope);
            puts.invokeExact(cstr.address());
        }
    }
}
