package app.snapshot_bitcake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYMarkerResponseMessage;
import servent.message.util.ChildResultCollector;
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

	// TODO Koristi ovo za "nesigurne istorije"
	private final Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

	private final Map<SnapshotID, Map<Integer, Integer>> giveHistories = new ConcurrentHashMap<>();
	private final Map<SnapshotID, Map<Integer, Integer>> getHistories = new ConcurrentHashMap<>();
	
	public LaiYangBitcakeManager() {
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			giveHistory.put(neighbor, 0);
			getHistory.put(neighbor, 0);
		}

		for (Integer initId : AppConfig.initiatorIds) {
			initMapForSnapshot(initId, 0);
		}
	}
	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	public void markerEvent(int collectorId, SnapshotCollector snapshotCollector, int version, int parent) {
		synchronized (AppConfig.versionLock) {
			int oldVersion = AppConfig.initiatorVersions.get(collectorId);
			AppConfig.initiatorVersions.put(collectorId, version);
			initMapForSnapshot(collectorId, AppConfig.initiatorVersions.get(collectorId));

			// TODO Prepraviti ovaj hak, mada mozda i ne moras
			for (int initiator : AppConfig.initiatorIds) {
				initMapForSnapshot(initiator, AppConfig.initiatorVersions.get(initiator) + 1);
			}

			AppConfig.region.set(collectorId);
			AppConfig.treeParent.set(parent);

			recordedAmount = getCurrentBitcakeAmount();

//			LYSnapshotResult snapshotResult = new LYSnapshotResult(
//					AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory);

			LYSnapshotResult snapshotResult1 = new LYSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistories.get(new SnapshotID(collectorId, oldVersion)),
					getHistories.get(new SnapshotID(collectorId, oldVersion)));
			
			if (collectorId == AppConfig.myServentInfo.getId()) {
				snapshotCollector.addLYSnapshotInfo(
						AppConfig.myServentInfo.getId(),
						snapshotResult1);
			} else {
				Thread helper = new Thread(new ChildResultCollector(AppConfig.myServentInfo.getNeighbors().size(),
						snapshotResult1, parent));
				helper.start();
			}

			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor),
						collectorId).makeMeASender();
				MessageUtil.sendMessage(clMarker);

				if (neighbor == parent) {
					LYMarkerResponseMessage lyMarkerResponse = new LYMarkerResponseMessage(AppConfig.myServentInfo,
							AppConfig.getInfoById(neighbor), -1);
					MessageUtil.sendMessage(lyMarkerResponse);
				} else {

					Message lyMarkerResponse = new LYMarkerResponseMessage(AppConfig.myServentInfo,
							AppConfig.getInfoById(neighbor), collectorId) {
						private static final long serialVersionUID = -269153763891389772L;
					}.makeMeASender();
					MessageUtil.sendMessage(lyMarkerResponse);

				}
//				try {
//					/*
//					 * This sleep is here to artificially produce some white node -> red node messages.
//					 * Not actually recommended, as we are sleeping while we have colorLock.
//					 */
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
			}
		}
	}

	private void initMapForSnapshot(int initId, int version) {
		Map<Integer, Integer> newGiveMap = new ConcurrentHashMap<>();
		Map<Integer, Integer> newGetMap = new ConcurrentHashMap<>();
		for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			newGiveMap.put(neighbor, 0);
			newGetMap.put(neighbor, 0);
		}
		giveHistories.put(new SnapshotID(initId, version), newGiveMap);
		getHistories.put(new SnapshotID(initId, version), newGetMap);
	}
	
	private static class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
		
		private final int valueToAdd;
		
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

	public void recordGiveTransaction(SnapshotID snapshotID, int neighbor, int amount) {
		giveHistories.get(snapshotID).compute(neighbor, new MapValueUpdater(amount));
	}

	public void recordGetTransaction(SnapshotID snapshotID, int neighbor, int amount) {
		getHistories.get(snapshotID).compute(neighbor, new MapValueUpdater(amount));
	}
}
