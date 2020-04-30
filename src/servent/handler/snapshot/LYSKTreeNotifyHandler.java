package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.LYSKTreeNotifyMessage;
import servent.message.util.MessageUtil;

public class LYSKTreeNotifyHandler implements MessageHandler {

    private final Message clientMessage;
    private final BitcakeManager bitcakeManager;

    public LYSKTreeNotifyHandler(Message clientMessage, BitcakeManager bitcakeManager) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void run() {
        LYSKTreeNotifyMessage lyskTreeNotifyMessage = (LYSKTreeNotifyMessage)clientMessage;

        for (int child : AppConfig.treeChildren) {
            LYSKTreeNotifyMessage toSend = new LYSKTreeNotifyMessage(AppConfig.myServentInfo, AppConfig.getInfoById(child),
                    lyskTreeNotifyMessage.getInitiators());

            MessageUtil.sendMessage(toSend);
        }

        synchronized (AppConfig.versionLock) {
            for (int initiator : lyskTreeNotifyMessage.getInitiators()) {
                int oldVersion = AppConfig.initiatorVersions.get(initiator);
                AppConfig.initiatorVersions.put(initiator, oldVersion + 1);
            }

            ((LaiYangBitcakeManager)bitcakeManager).setFromUnvertainHistory();
            ((LaiYangBitcakeManager)bitcakeManager).flushUncertainHistory();

            AppConfig.region.set(-1);
            AppConfig.treeParent.set(-1);
            AppConfig.treeChildren.clear();
        }
    }
}
