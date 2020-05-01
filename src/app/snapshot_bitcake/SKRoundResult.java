package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;

public class SKRoundResult {

    private final int sender;
    private final Map<Integer, List<LYSnapshotResult>> lySnapshotResult;
    private final int roundNumber;

    public SKRoundResult(int sender, Map<Integer, List<LYSnapshotResult>> lySnapshotResult, int messageNo) {
        this.sender = sender;
        this.lySnapshotResult = lySnapshotResult;
        this.roundNumber = messageNo;
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
}
