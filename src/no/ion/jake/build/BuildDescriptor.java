package no.ion.jake.build;

import no.ion.jake.io.FileSet;

import java.util.List;

public interface BuildDescriptor {
    /** The build depends on these file sets. */
    List<FileSet> fileSetDependencies();

    List<Module> moduleDependencies();


}
