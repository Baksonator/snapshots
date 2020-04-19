package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotID;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.List;

public class LYSKNeighborNotifyMessage extends BasicMessage {

    private static final long serialVersionUID = -472280305312676054L;

    private final List<LYSnapshotResult> lySnapshotResults;

    public LYSKNeighborNotifyMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                                     List<LYSnapshotResult> lySnapshotResults) {
        super(MessageType.LYSK_NEIGHBOR_NOTIFY, originalSenderInfo, receiverInfo);

        this.lySnapshotResults = lySnapshotResults;
    }

    public List<LYSnapshotResult> getLySnapshotResults() { return lySnapshotResults; }
}
