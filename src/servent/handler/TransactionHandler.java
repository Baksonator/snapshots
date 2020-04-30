package servent.handler;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotID;
import servent.message.Message;
import servent.message.MessageType;

public class TransactionHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;
	
	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}

			synchronized (AppConfig.versionLock) {
				bitcakeManager.addSomeBitcakes(amountNumber);
				if (bitcakeManager instanceof LaiYangBitcakeManager) {
					LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)bitcakeManager;
					
//					lyFinancialManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
//					for (SnapshotID snapshotID : clientMessage.getSnapshotIDS()) {
//						lyFinancialManager.recordGetTransaction(snapshotID, clientMessage.getOriginalSenderInfo().getId(),
//								amountNumber);
//					}

					if (clientMessage.getUncertainty()) {
						if (AppConfig.region.get() != -1) {
							lyFinancialManager.recordUncertainGetTransaction(clientMessage.getOriginalSenderInfo().getId(),
									amountNumber);
						} else {
							for (Integer initiator : AppConfig.initiatorIds) {
								SnapshotID snapshotID = new SnapshotID(initiator, AppConfig.initiatorVersions.get(initiator));
								lyFinancialManager.recordGetTransaction(snapshotID, clientMessage.getOriginalSenderInfo().getId(),
										amountNumber);
							}
						}
					} else {
						for (SnapshotID snapshotID : clientMessage.getSnapshotIDS()) {
							lyFinancialManager.recordGetTransaction(snapshotID, clientMessage.getOriginalSenderInfo().getId(),
									amountNumber);
						}
					}
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
