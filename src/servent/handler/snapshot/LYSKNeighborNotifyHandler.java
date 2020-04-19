package servent.handler.snapshot;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.LYSKNeighborNotifyMessage;

public class LYSKNeighborNotifyHandler implements MessageHandler {

    private final Message clientMessage;

    public LYSKNeighborNotifyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = (LYSKNeighborNotifyMessage)clientMessage;

        AppConfig.regionResponses.add(lyskNeighborNotifyMessage.getLySnapshotResults());
    }
}
