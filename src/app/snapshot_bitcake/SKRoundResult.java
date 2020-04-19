package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;

public class SKRoundResult {

    private final int sender;
    private final Map<Integer, List<LYSnapshotResult>> lySnapshotResult;

    public SKRoundResult(int sender, Map<Integer, List<LYSnapshotResult>> lySnapshotResult) {
        this.sender = sender;
        this.lySnapshotResult = lySnapshotResult;
    }

    public int getSender() {
        return sender;
    }

    public Map<Integer, List<LYSnapshotResult>> getLySnapshotResult() {
        return lySnapshotResult;
    }

}
