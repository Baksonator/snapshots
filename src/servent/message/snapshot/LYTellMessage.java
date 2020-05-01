package servent.message.snapshot;

import java.util.List;
import java.util.Map;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotID;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class LYTellMessage extends BasicMessage {

	private static final long serialVersionUID = 3116394054726162318L;

	private final List<LYSnapshotResult> lySnapshotResults;

	private final Map<Integer, List<LYSnapshotResult>> allLySnapshotResults;
	
	public LYTellMessage(ServentInfo sender, ServentInfo receiver, List<LYSnapshotResult> lySnapshotResults,
						 Map<Integer, List<LYSnapshotResult>> allLySnapshotResults) {
		super(MessageType.LY_TELL, sender, receiver);
		
		this.lySnapshotResults = lySnapshotResults;
		this.allLySnapshotResults = allLySnapshotResults;
	}
	
	private LYTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver,
						  List<ServentInfo> routeList, String messageText, List<SnapshotID> snapshotIDS,
						  int messageId, List<LYSnapshotResult> lySnapshotResults,
						  Map<Integer, List<LYSnapshotResult>> allLySnapshotResults) {
		super(messageType, sender, receiver, routeList, messageText, snapshotIDS, messageId);
		this.lySnapshotResults = lySnapshotResults;
		this.allLySnapshotResults = allLySnapshotResults;
	}

	public List<LYSnapshotResult> getLYSnapshotResults() {
		return lySnapshotResults;
	}

	public Map<Integer, List<LYSnapshotResult>> getAllLySnapshotResults() {
		return allLySnapshotResults;
	}

	@Override
	public Message setSnapshotIDS() {
		List<SnapshotID> snapshotIDS = AppConfig.getSnapshotIDS();

		return new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(), getRoute(),
				getMessageText(), snapshotIDS, getMessageId(), getLYSnapshotResults(), getAllLySnapshotResults());
	}
}
