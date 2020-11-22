# jake
Java make

## Performance

Building `yolean`, `testutil`, and `vespajlib` with maven versus jake using the shell's `time` builtin:

```
+------+--------+------+
| time |  mvn   | jake |
+------+--------+------+
| real |   19.5 |  7.8 |
| user | 1:37.1 | 51.3 |
| sys  |    2.8 |  1.1 |
+------+--------+------+
```

### Maven

```
vespa$ time mvn -nsu -pl yolean,testutil,vespajlib install
...
[INFO] Reactor Summary for yolean 7-SNAPSHOT:
[INFO] 
[INFO] yolean ............................................. SUCCESS [  4.763 s]
[INFO] testutil ........................................... SUCCESS [  2.999 s]
[INFO] vespajlib .......................................... SUCCESS [ 10.285 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  18.868 s
[INFO] Finished at: 2020-11-22T15:11:17+01:00
[INFO] ------------------------------------------------------------------------

real	0m19,517s
user	1m37,143s
sys	0m2,809s
```

### Jake

```
vespa$ time jake
  testutil compiled 9 files to target/classes in 0.462 s
  testutil compiled 2 files to target/test-classes in 0.059 s
  testutil ran 5 tests in 0.011 s
  testutil archived target/testutil.jar in 0.028 s
  testutil archived target/testutil-sources.jar in 0.001 s
  testutil wrote javadoc to target/apidocs in 0.376 s
  testutil archived target/testutil-javadoc.jar in 0.044 s
  testutil installed target/testutil.jar as com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT.jar
  testutil installed pom.xml as com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT.pom
  testutil installed target/testutil-sources.jar as com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT-sources.jar
  testutil installed target/testutil-javadoc.jar as com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT-javadoc.jar
  yolean compiled 28 files to target/classes in 0.209 s
  yolean compiled 10 files to target/test-classes in 0.169 s
  yolean wrote target/test-classes/bundle-plugin.bundle-classpath-mappings.json in 0.085 s
  yolean ran 75 tests in 0.254 s
W yolean This project does not have jdisc_core as provided dependency, so the generated 'Import-Package' OSGi header may be missing important packages.
  yolean wrote target/classes/META-INF/MANIFEST.MF in 0.037 s
  yolean wrote javadoc to target/apidocs in 0.204 s
  yolean archived target/yolean-sources.jar in 0.002 s
  yolean archived target/yolean-javadoc.jar in 0.048 s
  yolean archived target/yolean.jar in 0.003 s
  yolean archived target/yolean-jar-with-dependencies.jar in 0.002 s
  yolean checked abi compatibility in 0.027 s
  yolean installed target/yolean.jar as com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT.jar
  yolean installed pom.xml as com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT.pom
  yolean installed target/yolean-sources.jar as com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT-sources.jar
  yolean installed target/yolean-javadoc.jar as com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT-javadoc.jar
  vespajlib compiled 277 files to target/classes in 0.992 s
  vespajlib compiled 136 files to target/test-classes in 0.927 s
  vespajlib wrote target/test-classes/bundle-plugin.bundle-classpath-mappings.json in 0.000 s
  vespajlib ran 718 tests in 1.952 s
W vespajlib This project does not have jdisc_core as provided dependency, so the generated 'Import-Package' OSGi header may be missing important packages.
  vespajlib wrote target/classes/META-INF/MANIFEST.MF in 0.102 s
  vespajlib wrote javadoc to target/apidocs in 1.152 s
  vespajlib archived target/vespajlib-sources.jar in 0.022 s
  vespajlib archived target/vespajlib-javadoc.jar in 0.157 s
  vespajlib archived target/vespajlib.jar in 0.034 s
  vespajlib archived target/vespajlib-jar-with-dependencies.jar in 0.034 s
  vespajlib checked abi compatibility in 0.024 s
  vespajlib installed target/vespajlib.jar as com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT.jar
  vespajlib installed pom.xml as com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT.pom
  vespajlib installed target/vespajlib-sources.jar as com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT-sources.jar
  vespajlib installed target/vespajlib-javadoc.jar as com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT-javadoc.jar
  SUCCESS

real	0m7,785s
user	0m51,333s
sys	0m1,097s
```
