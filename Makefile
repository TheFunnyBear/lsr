JAR_NAME=lsr.jar
KOTLIN_FILE=lsr.kt
JSOUP_JAR=jsoup-1.15.4.jar
MAIN_CLASS=LsrKt

.PHONY: all run clean

all: $(JAR_NAME)

$(JAR_NAME): $(KOTLIN_FILE) $(JSOUP_JAR)
	kotlinc $(KOTLIN_FILE) -cp $(JSOUP_JAR) -include-runtime -d $(JAR_NAME)

run: $(JAR_NAME)
	java -cp $(JAR_NAME):$(JSOUP_JAR) $(MAIN_CLASS)

clean:
	rm -f $(JAR_NAME)
