.SUFFIXES:
.SUFFIXES: .class .java
.PHONY: all run run2 run3 clean target/debug/libv1.so target/debug/libv2.so

all: run run2 run3

run3: Main3.class
	java --add-modules jdk.incubator.foreign --enable-native-access=ALL-UNNAMED Main3

run2: Main2.class target/debug/v1 target/debug/v2
	java --add-modules jdk.incubator.foreign --enable-native-access=ALL-UNNAMED Main2

run: Main.class target/debug/v1 target/debug/v2
	-java --add-modules jdk.incubator.foreign --enable-native-access=ALL-UNNAMED Main

.java.class:
	javac --add-modules jdk.incubator.foreign $<

target/debug/v1:
	cargo build -p v1

target/debug/v2:
	cargo build -p v2

clean:
	$(RM) Main.class Main2.class
	cargo clean
