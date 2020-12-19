# jake

Java make

While jake currently only builds (parts of) Vespa, the idea is to extract the Vespa-related code to its own repository ("plugin").

## Performance

Building `yolean`, `testutil`, and `vespajlib` with maven (`mvn -nsu yolean,testutil,vespajlib -T 1C install`) versus jake using the shell's `time` builtin:

```
+------+--------+--------+
| time |  mvn   |  jake  |
+------+--------+--------+
| real |   16.3 |    6.2 |
| user | 2:09.8 | 1:01.3 |
| sys  |    3.3 |    1.3 |
+------+--------+--------+
```

### Maven

```
vespa$ time mvn -nsu -pl yolean,testutil,vespajlib install
...
[INFO] Reactor Summary for yolean 7-SNAPSHOT:
[INFO] 
[INFO] yolean ............................................. SUCCESS [  4.757 s]
[INFO] testutil ........................................... SUCCESS [  3.987 s]
[INFO] vespajlib .......................................... SUCCESS [ 10.104 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.688 s (Wall Clock)
[INFO] Finished at: 2020-12-20T00:18:20+01:00
[INFO] ------------------------------------------------------------------------

real	0m16,348s
user	2m9,961s
sys	0m3,231s

### Jake

```
vespa$ time jake
  yolean found 28 java files files
  testutil found 2 test source files
  testutil found 9 java files files
  yolean found 10 test source files
  vespajlib found 136 test source files
  vespajlib found 277 java files files
  yolean archived target/yolean-javadoc.jar in 0.029 s
  testutil archived target/testutil-sources.jar in 0.027 s
  yolean archived target/yolean-sources.jar in 0.029 s
  testutil archived target/testutil-javadoc.jar in 0.027 s
  vespajlib archived target/vespajlib-sources.jar in 0.052 s
  vespajlib archived target/vespajlib-javadoc.jar in 0.055 s
  yolean wrote target/test-classes/bundle-plugin.bundle-classpath-mappings.json in 0.141 s
  testutil compiled 9 files to target/classes in 0.414 s
  testutil archived target/testutil.jar in 0.002 s
  testutil compiled 2 files to target/test-classes in 0.066 s
  yolean compiled 28 files to target/classes in 0.510 s
  testutil ran 5 tests in 0.013 s
  testutil installed target/testutil-javadoc.jar as com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT-javadoc.jar
  testutil installed target/testutil-sources.jar as com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT-sources.jar
  testutil installed pom.xml as com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT.pom
  testutil installed target/testutil.jar as com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT.jar
W yolean This project does not have jdisc_core as provided dependency, so the generated 'Import-Package' OSGi header may be missing important packages.
  yolean wrote target/classes/META-INF/MANIFEST.MF in 0.312 s
  yolean archived target/yolean-jar-with-dependencies.jar in 0.004 s
  yolean archived target/yolean.jar in 0.004 s
  yolean compiled 10 files to target/test-classes in 0.335 s
  testutil wrote javadoc to target/apidocs in 0.431 s
  yolean checked abi compatibility in 0.040 s
  yolean wrote javadoc to target/apidocs in 0.372 s
  yolean ran 75 tests in 0.332 s
  yolean installed target/yolean-sources.jar as com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT-sources.jar
  yolean installed pom.xml as com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT.pom
  yolean installed target/yolean.jar as com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT.jar
  yolean installed target/yolean-javadoc.jar as com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT-javadoc.jar
  vespajlib wrote target/test-classes/bundle-plugin.bundle-classpath-mappings.json in 0.000 s
  vespajlib compiled 277 files to target/classes in 1.168 s
W vespajlib This project does not have jdisc_core as provided dependency, so the generated 'Import-Package' OSGi header may be missing important packages.
  vespajlib wrote target/classes/META-INF/MANIFEST.MF in 0.110 s
  vespajlib archived target/vespajlib.jar in 0.086 s
  vespajlib archived target/vespajlib-jar-with-dependencies.jar in 0.089 s
  vespajlib checked abi compatibility in 0.028 s
  vespajlib compiled 136 files to target/test-classes in 1.005 s
  vespajlib wrote javadoc to target/apidocs in 1.453 s
  vespajlib ran 718 tests in 1.964 s
  vespajlib installed pom.xml as com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT.pom
  vespajlib installed target/vespajlib-javadoc.jar as com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT-javadoc.jar
  vespajlib installed target/vespajlib-sources.jar as com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT-sources.jar
  vespajlib installed target/vespajlib.jar as com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT.jar
  SUCCESS

real	0m6,138s
user	1m1,279s
sys	0m1,304s
```
