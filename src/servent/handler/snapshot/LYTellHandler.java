package servent.handler.snapshot;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;

public class LYTellHandler implements MessageHandler {

	private final Message clientMessage;
	
	public LYTellHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LY_TELL) {
			LYTellMessage lyTellMessage = (LYTellMessage)clientMessage;

			AppConfig.treeChildren.add(clientMessage.getOriginalSenderInfo().getId());
			AppConfig.childrenResponses.add(lyTellMessage.getLYSnapshotResults());
			AppConfig.childrenResponsesAlt.add(lyTellMessage.getAllLySnapshotResults());
		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
