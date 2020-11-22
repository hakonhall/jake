.PHONY: all

JAR := jar/no.ion.jake-0.0.1.jar
CCP := lib/junit-4.12.jar:lib/bundle-plugin.jar:lib/abi-check-plugin.jar

JAVA_FILES := $(shell find src -name '*.java')

all: $(JAR)
	# If this fails: Check out vespa-engine/vespa (and fix a bunch of stuff)
	test -d ../../vespa-engine/vespa
	jake -p ../../vespa-engine/vespa

$(JAR): jar $(JAVA_FILES)
	javac -cp $(CCP) -d classes $(JAVA_FILES)
	jar -c -f $(JAR) -C classes .

jar:
	mkdir jar

clean:
	rm -rf jar classes
