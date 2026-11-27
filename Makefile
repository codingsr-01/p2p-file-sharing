# Build the two runnable jars (Java 14+ is required for the switch expressions).
#   make          -> build/IndexServer.jar, build/PeerClientServer.jar
#   make clean
JAVAC ?= javac
JAR   ?= jar
SRC   := $(shell find src -name '*.java')

all: build/IndexServer.jar build/PeerClientServer.jar

build/classes: $(SRC)
	@mkdir -p build/classes
	$(JAVAC) --release 17 -encoding UTF-8 -d build/classes $(SRC)
	@touch build/classes

build/IndexServer.jar: build/classes
	$(JAR) cfe $@ NodoIndexServer.IndexServer -C build/classes .

build/PeerClientServer.jar: build/classes
	$(JAR) cfe $@ NodoPeer.PeerClientServer -C build/classes .

clean:
	rm -rf build

.PHONY: all clean
