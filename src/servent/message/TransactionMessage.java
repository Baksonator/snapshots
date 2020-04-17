package servent.message;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotID;

import java.util.List;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends BasicMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private transient BitcakeManager bitcakeManager;

	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount));
		this.bitcakeManager = bitcakeManager;
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		int amount = Integer.parseInt(getMessageText());

		bitcakeManager.takeSomeBitcakes(amount);
		if (bitcakeManager instanceof LaiYangBitcakeManager) {
//		if (bitcakeManager instanceof LaiYangBitcakeManager && isWhite()) {
			LaiYangBitcakeManager lyBitcakeManager = (LaiYangBitcakeManager)bitcakeManager;
			
			lyBitcakeManager.recordGiveTransaction(getReceiverInfo().getId(), amount);
			for (SnapshotID snapshotID : getSnapshotIDS()) {
				lyBitcakeManager.recordGiveTransaction(snapshotID, getReceiverInfo().getId(),
						amount);
			}
		}
	}

	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}

	private TransactionMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver,
							   boolean white, List<ServentInfo> routeList, String messageText, List<SnapshotID> snapshotIDS,
							   int messageId, BitcakeManager bitcakeManager) {
		super(messageType, sender, receiver, white, routeList, messageText, snapshotIDS, messageId);
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public Message setSnapshotIDS() {
		List<SnapshotID> snapshotIDS = AppConfig.getSnapshotIDS();

		Message toReturn = new TransactionMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
				isWhite(), getRoute(), getMessageText(), snapshotIDS, getMessageId(), getBitcakeManager());

		return toReturn;
	}
}
