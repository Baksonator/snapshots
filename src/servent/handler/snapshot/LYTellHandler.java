package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;

public class LYTellHandler implements MessageHandler {

	private Message clientMessage;
	private SnapshotCollector snapshotCollector;
	
	public LYTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	/**
	 * TODO Izmeniti ponasanje ovoga, samo ako smo inicijator treba da zapisujemo, u suprotnom saljemo
	 * poruku dalje, tj. prepustamo kontrolu threadu spomenutom u {@link app.snapshot_bitcake.LaiYangBitcakeManager}
	 * Kada se kaze prepustamo kontrolu, tu tipa radimo upisivanje u neki blokirajuci red ili tako nesto
	 */
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LY_TELL) {
			LYTellMessage lyTellMessage = (LYTellMessage)clientMessage;
			
			snapshotCollector.addLYSnapshotInfo(
					lyTellMessage.getOriginalSenderInfo().getId(),
					lyTellMessage.getLYSnapshotResult());
		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
