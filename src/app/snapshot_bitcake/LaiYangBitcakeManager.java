package app.snapshot_bitcake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

public class LaiYangBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}
	
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}
	
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}
	
	private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
	private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();
	
	public LaiYangBitcakeManager() {
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			giveHistory.put(neighbor, 0);
			getHistory.put(neighbor, 0);
		}
	}
	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	public void markerEvent(int collectorId, SnapshotCollector snapshotCollector) {
		synchronized (AppConfig.colorLock) {
			AppConfig.isWhite.set(false);
			recordedAmount = getCurrentBitcakeAmount();

			LYSnapshotResult snapshotResult = new LYSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory);
			
			if (collectorId == AppConfig.myServentInfo.getId()) {
				snapshotCollector.addLYSnapshotInfo(
						AppConfig.myServentInfo.getId(),
						snapshotResult);
			} else {
			
				Message tellMessage = new LYTellMessage(
						AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), snapshotResult);
				
				MessageUtil.sendMessage(tellMessage);
			}
			
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId);
				MessageUtil.sendMessage(clMarker);
				try {
					/*
					 * This sleep is here to artificially produce some white node -> red node messages.
					 * Not actually recommended, as we are sleeping while we have colorLock.
					 */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
		
		private int valueToAdd;
		
		public MapValueUpdater(int valueToAdd) {
			this.valueToAdd = valueToAdd;
		}
		
		@Override
		public Integer apply(Integer key, Integer oldValue) {
			return oldValue + valueToAdd;
		}
	}
	
	public void recordGiveTransaction(int neighbor, int amount) {
		giveHistory.compute(neighbor, new MapValueUpdater(amount));
	}
	
	public void recordGetTransaction(int neighbor, int amount) {
		getHistory.compute(neighbor, new MapValueUpdater(amount));
	}
}
