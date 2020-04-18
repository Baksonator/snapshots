package servent.message.snapshot;

import java.util.ArrayList;
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
	private LYSnapshotResult lySnapshotResult;
	
	public LYTellMessage(ServentInfo sender, ServentInfo receiver, LYSnapshotResult lySnapshotResult) {
		super(MessageType.LY_TELL, sender, receiver);
		
		this.lySnapshotResult = lySnapshotResult;
	}
	
	private LYTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver,
						  List<ServentInfo> routeList, String messageText, List<SnapshotID> snapshotIDS,
						  int messageId, LYSnapshotResult lySnapshotResult) {
		super(messageType, sender, receiver, routeList, messageText, snapshotIDS, messageId);
		this.lySnapshotResult = lySnapshotResult;
	}

	public LYSnapshotResult getLYSnapshotResult() {
		return lySnapshotResult;
	}

	@Override
	public Message setSnapshotIDS() {
		List<SnapshotID> snapshotIDS = AppConfig.getSnapshotIDS();

		Message toReturn = new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
				getRoute(), getMessageText(), snapshotIDS, getMessageId(), getLYSnapshotResult());

		return toReturn;
	}
}
