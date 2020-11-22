# Files

* vespa.html:  For each Vespa module, list it's dependencies - both other modules in the same Vespa project, and external dependencies
* deps.html: Dependencies between Vespa modules.
* vespa-module-deps.html:  Vespa modules grouped, such that each group can be built once its prerequisite groups have been built.  
Useful to analyze the minimum number of modules that needs to be built in sequence and the concurrency level that can be achieved.
* module.svg:  What a container-plugin module depends on, builds, and outputs.
