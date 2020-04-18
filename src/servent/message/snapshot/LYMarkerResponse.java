package servent.message.snapshot;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.ArrayList;
import java.util.List;

public class LYMarkerResponse extends BasicMessage {

    private static final long serialVersionUID = 75093875792492222L;

    public LYMarkerResponse(ServentInfo originalSenderInfo, ServentInfo receiverInfo, int response) {
        super(MessageType.LY_MARKER_RESPONSE, originalSenderInfo, receiverInfo, String.valueOf(response));
    }

}
