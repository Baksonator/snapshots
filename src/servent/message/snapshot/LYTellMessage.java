package servent.message.snapshot;

import java.util.List;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotID;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class LYTellMessage extends BasicMessage {

	private static final long serialVersionUID = 3116394054726162318L;

	// TODO Staviti da se prati i lista susednih regiona
	private List<LYSnapshotResult> lySnapshotResults;
	
	public LYTellMessage(ServentInfo sender, ServentInfo receiver, List<LYSnapshotResult> lySnapshotResults) {
		super(MessageType.LY_TELL, sender, receiver);
		
		this.lySnapshotResults = lySnapshotResults;
	}
	
	private LYTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver,
						  List<ServentInfo> routeList, String messageText, List<SnapshotID> snapshotIDS,
						  int messageId, List<LYSnapshotResult> lySnapshotResults) {
		super(messageType, sender, receiver, routeList, messageText, snapshotIDS, messageId);
		this.lySnapshotResults = lySnapshotResults;
	}

	public List<LYSnapshotResult> getLYSnapshotResults() {
		return lySnapshotResults;
	}

	@Override
	public Message setSnapshotIDS() {
		List<SnapshotID> snapshotIDS = AppConfig.getSnapshotIDS();

		Message toReturn = new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
				getRoute(), getMessageText(), snapshotIDS, getMessageId(), getLYSnapshotResults());

		return toReturn;
	}
}
