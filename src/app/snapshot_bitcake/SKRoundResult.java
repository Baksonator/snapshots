package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;

public class SKRoundResult {

    private final int sender;
    private final Map<Integer, List<LYSnapshotResult>> lySnapshotResult;
    private final int roundNumber;
    private final Map<Integer, List<LYSnapshotResult>> allLySnapshotResults;

    public SKRoundResult(int sender, Map<Integer, List<LYSnapshotResult>> lySnapshotResult, int messageNo,
                         Map<Integer, List<LYSnapshotResult>> allLySnapshotResults) {
        this.sender = sender;
        this.lySnapshotResult = lySnapshotResult;
        this.roundNumber = messageNo;
        this.allLySnapshotResults = allLySnapshotResults;
    }

    public int getSender() {
        return sender;
    }

    public Map<Integer, List<LYSnapshotResult>> getLySnapshotResult() {
        return lySnapshotResult;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Map<Integer, List<LYSnapshotResult>> getAllLySnapshotResults() { return allLySnapshotResults; }
}
