AUTOMAKE_OPTIONS = foreign

all-local:
	$(GRADLE) -Pjavacflags="$(JAVACFLAGS)" assemble

install-data-hook:
	@echo "Note: Jars are currently installed by toplevel 'make install'"

clean-local:
	$(GRADLE) clean

javadoc:
	$(GRADLE) javadoc
	mv build/docs/javadoc doc # backward compat
