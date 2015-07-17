gerrit-dataset-miner
=====================

Java application for mining datasets from Gerrit code review system.


How to
-------------
1. Setup MSSQL Server and create an empty database.
2. Build a JAR file of the project, put sqljdbc JAR next to it.
3. Edit file symbols.xml and copy it next to the JAR file of the project.

 3.a. Add a connection string to the freshly created database. For an example, please, see default symbols.xml file in the project's directory:

    sqlserver://localhost;databaseName=android-gerrit-dataset;integratedSecurity=true;

 3.b. Setup a query for querying changes on Gerrit.
 3.c. Set up other settings in the properties file (TBA).
4. Run the JAR file and specify your symbols after the argument "-p", e.g.:

    java -cp sqljdbc41.jar;gerrit-dataset-miner.jar konopka.gerrit.Main -l log4j.properties -p symbols.xml

 4.a. The Log4j properties may be edited in similar way. It gets loaded with the argument "-l".






Dependencies
-------------
* [gerrit-rest-java-client]
* [MSSQL JDBC connector]
* [SLF4J]

[gerrit-rest-java-client]: https://github.com/uwolfer/gerrit-rest-java-client
[MSSQL JDBC connector]: https://msdn.microsoft.com/en-us/library/dn425070%28v=sql.10%29.aspx
[SLF4J]: https://logging.apache.org/log4j/2.0/log4j-slf4j-impl/index.html

[Download datasets]
--------------------
Currently, Eclipse and Android are available, others are coming.
[Download datasets]: http://www2.fiit.stuba.sk/~konopka/gerrit

About
------------
Created by Martin Konopka
Developed with the IntelliJ Idea IDE
