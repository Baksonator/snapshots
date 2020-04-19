package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.List;

public class LYSKTreeNotifyMessage extends BasicMessage {

    private static final long serialVersionUID = 1199118403477053436L;

    private final List<Integer> initiators;

    public LYSKTreeNotifyMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, List<Integer> initiators) {
        super(MessageType.LYSK_TREE_NOTIFY, originalSenderInfo, receiverInfo);

        this.initiators = initiators;
    }

    public List<Integer> getInitiators() { return initiators; }
}
