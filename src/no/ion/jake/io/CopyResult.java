package no.ion.jake.io;

public class CopyResult {
    private final int numCopied;

    public static CopyResult nothingToCopy() {
        return new CopyResult(0);
    }

    public static CopyResult copiedFiles(int copied) {
        return new CopyResult(copied);
    }

    private CopyResult(int numCopied) {
        this.numCopied = numCopied;
    }

    public int numCopied() { return numCopied; }
}
