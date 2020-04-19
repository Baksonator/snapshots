package servent.handler.snapshot;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.LYSKTreeNotifyMessage;
import servent.message.util.MessageUtil;

public class LYSKTreeNotifyHandler implements MessageHandler {

    private final Message clientMessage;

    public LYSKTreeNotifyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        LYSKTreeNotifyMessage lyskTreeNotifyMessage = (LYSKTreeNotifyMessage)clientMessage;

        for (int child : AppConfig.treeChildren) {
            LYSKTreeNotifyMessage toSend = new LYSKTreeNotifyMessage(AppConfig.myServentInfo, AppConfig.getInfoById(child),
                    lyskTreeNotifyMessage.getInitiators());

            MessageUtil.sendMessage(toSend);
        }

        // TODO Mora i da se azurira i isprazni "nesigurna" istorija
        for (int initiator : lyskTreeNotifyMessage.getInitiators()) {
            int oldVersion = AppConfig.initiatorVersions.get(initiator);
            AppConfig.initiatorVersions.put(initiator, oldVersion + 1);
        }

        AppConfig.region.set(-1);
        AppConfig.treeParent.set(-1);
        AppConfig.treeChildren.clear();
    }
}
