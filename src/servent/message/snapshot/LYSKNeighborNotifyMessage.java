package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class LYSKNeighborNotifyMessage extends BasicMessage {

    private static final long serialVersionUID = -472280305312676054L;

    private final Map<Integer, List<LYSnapshotResult>> lySnapshotResults;
    private final int messageNo;

    public LYSKNeighborNotifyMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                                     Map<Integer, List<LYSnapshotResult>> lySnapshotResults, int messageNo) {
        super(MessageType.LYSK_NEIGHBOR_NOTIFY, originalSenderInfo, receiverInfo);

        this.lySnapshotResults = lySnapshotResults;
        this.messageNo = messageNo;
    }

    public Map<Integer, List<LYSnapshotResult>> getLySnapshotResults() { return lySnapshotResults; }

    public int getMessageNo() { return messageNo; }
}
