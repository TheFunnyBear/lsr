JAR_NAME=lsr.jar
KOTLIN_FILE=lsr.kt
JSOUP_JAR=jsoup-1.15.4.jar
SQLITE_JAR=sqlite-jdbc-3.45.1.0.jar
SLF4J_JAR=slf4j-api-2.0.13.jar
MAIN_CLASS=LsrKt

.PHONY: all run clean deps build

all: deps build

build: $(KOTLIN_FILE) $(JSOUP_JAR) $(SQLITE_JAR) $(SLF4J_JAR)
	kotlinc $(KOTLIN_FILE) -cp "$(JSOUP_JAR):$(SQLITE_JAR):$(SLF4J_JAR)" -include-runtime -d $(JAR_NAME)

run: build
	java -cp "$(JAR_NAME):$(JSOUP_JAR):$(SQLITE_JAR):$(SLF4J_JAR)" $(MAIN_CLASS)

clean:
	rm -f $(JAR_NAME)

deps:
	@echo "Checking dependencies..."
	@if [ ! -f $(JSOUP_JAR) ]; then \
		echo "Downloading $(JSOUP_JAR)..."; \
		curl -L -o $(JSOUP_JAR) https://repo1.maven.org/maven2/org/jsoup/jsoup/1.15.4/jsoup-1.15.4.jar; \
	else \
		echo "$(JSOUP_JAR) already exists."; \
	fi

	@if [ ! -f $(SQLITE_JAR) ]; then \
		echo "Downloading $(SQLITE_JAR)..."; \
		curl -L -o $(SQLITE_JAR) https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar; \
	else \
		echo "$(SQLITE_JAR) already exists."; \
	fi

	@if [ ! -f $(SLF4J_JAR) ]; then \
		echo "Downloading $(SLF4J_JAR)..."; \
		curl -L -o $(SLF4J_JAR) https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar; \
	else \
		echo "$(SLF4J_JAR) already exists."; \
	fi

