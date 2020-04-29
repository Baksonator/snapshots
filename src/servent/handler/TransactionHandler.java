package servent.handler;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotID;
import servent.message.Message;
import servent.message.MessageType;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private BitcakeManager bitcakeManager;
	
	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}

			// TODO Check this
//			bitcakeManager.addSomeBitcakes(amountNumber);
			synchronized (AppConfig.versionLock) {
				bitcakeManager.addSomeBitcakes(amountNumber);
				if (bitcakeManager instanceof LaiYangBitcakeManager) {
					LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)bitcakeManager;
					
//					lyFinancialManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
					for (SnapshotID snapshotID : clientMessage.getSnapshotIDS()) {
						lyFinancialManager.recordGetTransaction(snapshotID, clientMessage.getOriginalSenderInfo().getId(),
								amountNumber);
					}
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
