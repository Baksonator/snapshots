package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;


public class LYMarkerResponse extends BasicMessage {

    private static final long serialVersionUID = 75093875792492222L;

    public LYMarkerResponse(ServentInfo originalSenderInfo, ServentInfo receiverInfo, int response) {
        super(MessageType.LY_MARKER_RESPONSE, originalSenderInfo, receiverInfo, String.valueOf(response));
    }

}
