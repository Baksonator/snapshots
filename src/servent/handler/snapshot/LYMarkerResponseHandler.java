package servent.handler.snapshot;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYMarkerResponse;

public class LYMarkerResponseHandler implements MessageHandler {

    private final Message clientMessage;

    public LYMarkerResponseHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        AppConfig.neighborResponses.add(Integer.parseInt(clientMessage.getMessageText()));
    }
}
