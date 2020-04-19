package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

public class LYTellHandler implements MessageHandler {

	private final Message clientMessage;
	private final SnapshotCollector snapshotCollector;
	
	public LYTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LY_TELL) {
			LYTellMessage lyTellMessage = (LYTellMessage)clientMessage;

//			if (AppConfig.treeParent.get() == -1) {
//				for (LYSnapshotResult lySnapshotResult : lyTellMessage.getLYSnapshotResults()) {
//					snapshotCollector.addLYSnapshotInfo(
//							lySnapshotResult.getServentId(),
//							lySnapshotResult);
//				}
//			} else {
			AppConfig.treeChildren.add(clientMessage.getOriginalSenderInfo().getId());
			AppConfig.childrenResponses.add(lyTellMessage.getLYSnapshotResults());
//			}
		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
