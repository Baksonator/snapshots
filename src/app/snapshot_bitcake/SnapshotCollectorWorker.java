package app.snapshot_bitcake;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import servent.message.snapshot.LYSKNeighborNotifyMessage;
import servent.message.snapshot.LYSKTreeNotifyMessage;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private final AtomicBoolean collecting = new AtomicBoolean(false);

	private final Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();

	private int mySnapshotVersion = 0;

	private final Map<Integer, Integer> expectingMessage = new HashMap<>();
	
	private final BitcakeManager bitcakeManager;

	private Map<Integer, LYSnapshotResult> myMap;

	public SnapshotCollectorWorker() {
		bitcakeManager = new LaiYangBitcakeManager();
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (!collecting.get()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (!working) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */

			//1 send asks
			mySnapshotVersion++;
			((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this,
					mySnapshotVersion, -1);

			//2 wait for responses or finish
			int children = 0;
			Set<Integer> neighobringRegions = new HashSet<>();

			int k = 0;
			while (k < AppConfig.myServentInfo.getNeighbors().size()) {
				try {
					int res = AppConfig.neighborResponses.take();
					if (res == -1) {
						children++;
					} else if (res >= 0) {
						neighobringRegions.add(res);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				k++;
			}

			Map<Integer, List<LYSnapshotResult>> allLySnapshotResults = new HashMap<>();
			for (int initiator : AppConfig.initiatorIds) {
				allLySnapshotResults.put(initiator, new ArrayList<>());
			}

			List<LYSnapshotResult> lySnapshotResults = new ArrayList<>();
			while (children > 0) {
				try {
					lySnapshotResults.addAll(AppConfig.childrenResponses.take());

					Map<Integer, List<LYSnapshotResult>> oneMapResult = AppConfig.childrenResponsesAlt.take();
					for (Map.Entry<Integer, List<LYSnapshotResult>> entry : oneMapResult.entrySet()) {
						allLySnapshotResults.get(entry.getKey()).addAll(oneMapResult.get(entry.getKey()));
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				children--;
			}

			for (LYSnapshotResult lySnapshotResult : lySnapshotResults) {
				neighobringRegions.addAll(lySnapshotResult.getNeighboringRegions());
				addLYSnapshotInfo(lySnapshotResult.getServentId(), lySnapshotResult);
			}

			//3 round exchange of results
			neighobringRegions.remove(AppConfig.myServentInfo.getId());
			int expectedBlanks = neighobringRegions.size();
			Map<Integer, Set<Integer>> neighborHas = new HashMap<>();

			Map<Integer, Integer> sentMessageNumber = new HashMap<>();

			lySnapshotResults.add(collectedLYValues.get(AppConfig.myServentInfo.getId()));
			for (int initiator : AppConfig.initiatorIds) {
				allLySnapshotResults.get(initiator).add(myMap.get(initiator));
			}

			StringBuilder stringBuilder = new StringBuilder();
			String comma = "";
			for (Integer neighborInitiator : neighobringRegions) {
				Set<Integer> sent = new HashSet<>();
				sent.add(AppConfig.myServentInfo.getId());
				neighborHas.put(neighborInitiator, sent);

				Map<Integer, List<LYSnapshotResult>> toSend = new HashMap<>();
				toSend.put(AppConfig.myServentInfo.getId(), lySnapshotResults);

				LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(AppConfig.myServentInfo,
						AppConfig.getInfoById(neighborInitiator), toSend, 1, allLySnapshotResults);

				sentMessageNumber.put(neighborInitiator, 1);
				expectingMessage.put(neighborInitiator, 1);

				stringBuilder.append(comma);
				stringBuilder.append(neighborInitiator);
				comma = ",";

				MessageUtil.sendMessage(lyskNeighborNotifyMessage);
			}

			AppConfig.timestampedStandardPrint("My neighbors are: " + stringBuilder.toString());

			Set<Integer> gotResultsFrom = new HashSet<>();

			Map<Integer, List<LYSnapshotResult>> allResultsFromNeighbors = new HashMap<>();
			for (int initiator : AppConfig.initiatorIds) {
				allResultsFromNeighbors.put(initiator, new ArrayList<>());
			}

			Map<Integer, Boolean> sentBlanks = new HashMap<>();
			for (int neighborInitiator : neighobringRegions) {
				sentBlanks.put(neighborInitiator, false);
			}

			List<LYSnapshotResult> resultsFromNeighobrs = new ArrayList<>();
			while (true) {
				int roundAnswers = expectedBlanks;
				int blankAnswers = expectedBlanks;

				Map<Integer, Map<Integer, List<LYSnapshotResult>>> newRegionsAlt = new HashMap<>();

				Map<Integer, List<LYSnapshotResult>> newRegions = new HashMap<>();
				boolean gotNewResults = false;

				while (roundAnswers > 0) {

					while (AppConfig.regionResponses.isEmpty()) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						checkPendingMessages();
					}

					try {
						SKRoundResult newResult = AppConfig.regionResponses.take();

						if (newResult.getLySnapshotResult().isEmpty()) {
							blankAnswers--;
							expectedBlanks--;
							AppConfig.timestampedStandardPrint("Got blank message from: " +  newResult.getSender() +
									" in round: " + newResult.getRoundNumber());
						} else {

							StringBuilder sb = new StringBuilder();
							comma = "";

							for (Integer key : newResult.getLySnapshotResult().keySet()) {
								if (gotResultsFrom.add(key)) {
									newRegions.put(key, newResult.getLySnapshotResult().get(key));
									newRegionsAlt.put(key, newResult.getAllLySnapshotResults());
									neighborHas.get(newResult.getSender()).add(key);
									gotNewResults = true;
									resultsFromNeighobrs.addAll(newResult.getLySnapshotResult().get(key));
									for (int initiator : AppConfig.initiatorIds) {
										allResultsFromNeighbors.get(initiator).addAll(newResult.getAllLySnapshotResults().get(initiator));
									}
								}
								neighborHas.get(newResult.getSender()).add(key);
								sb.append(comma);
								sb.append(key);
								comma = ",";
							}

							AppConfig.timestampedStandardPrint("Got results for regions: " + sb.toString() + " from: "
									+ newResult.getSender() + " in round: " + newResult.getRoundNumber());
						}

						roundAnswers--;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

				if (blankAnswers == 0) {
					for (int neighborInitator : neighobringRegions) {
						if (!sentBlanks.get(neighborInitator)) {

							LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(
									AppConfig.myServentInfo, AppConfig.getInfoById(neighborInitator), new HashMap<>(),
									sentMessageNumber.get(neighborInitator) + 1, new HashMap<>());

							sentMessageNumber.put(neighborInitator, sentMessageNumber.get(neighborInitator) + 1);

							MessageUtil.sendMessage(lyskNeighborNotifyMessage);
						}
					}

					break;
				}

				Set<Integer> toRemove = new HashSet<>();

				if (!gotNewResults) {
					for (Integer neighborInitiator : neighobringRegions) {
						LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(
								AppConfig.myServentInfo, AppConfig.getInfoById(neighborInitiator), new HashMap<>(),
								sentMessageNumber.get(neighborInitiator) + 1, new HashMap<>());

						toRemove.add(neighborInitiator);

						sentBlanks.put(neighborInitiator, true);

						sentMessageNumber.put(neighborInitiator, sentMessageNumber.get(neighborInitiator) + 1);

						MessageUtil.sendMessage(lyskNeighborNotifyMessage);
					}
				} else {

					for (Integer neighborInitiator : neighobringRegions) {
						Map<Integer, List<LYSnapshotResult>> toSendAll = new HashMap<>();
						for (int initiators : AppConfig.initiatorIds) {
							toSendAll.put(initiators, new ArrayList<>());
						}

						boolean shouldSend = false;
						Map<Integer, List<LYSnapshotResult>> toSend = new HashMap<>();

						for (Integer newRegion : newRegions.keySet()) {
							if (!neighborHas.get(neighborInitiator).contains(newRegion)) {
								shouldSend = true;

								toSend.put(newRegion, newRegions.get(newRegion));

								Map<Integer, List<LYSnapshotResult>> mapToMerge = newRegionsAlt.get(newRegion);
								for (int initiator : AppConfig.initiatorIds) {
									toSendAll.get(initiator).addAll(mapToMerge.get(initiator));
								}
							}
						}

						if (!shouldSend) {
							LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(
									AppConfig.myServentInfo, AppConfig.getInfoById(neighborInitiator), new HashMap<>(),
									sentMessageNumber.get(neighborInitiator) + 1, new HashMap<>());

							toRemove.add(neighborInitiator);

							sentBlanks.put(neighborInitiator, true);

							sentMessageNumber.put(neighborInitiator, sentMessageNumber.get(neighborInitiator) + 1);

							MessageUtil.sendMessage(lyskNeighborNotifyMessage);
						} else {
							LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(
									AppConfig.myServentInfo, AppConfig.getInfoById(neighborInitiator), toSend,
									sentMessageNumber.get(neighborInitiator) + 1, toSendAll);

							sentMessageNumber.put(neighborInitiator, sentMessageNumber.get(neighborInitiator) + 1);

							MessageUtil.sendMessage(lyskNeighborNotifyMessage);
						}
					}
				}

				neighobringRegions.removeAll(toRemove);

				expectingMessage.replaceAll((k1, v) -> expectingMessage.get(k1) + 1);

			}

//			for (LYSnapshotResult lySnapshotResult : resultsFromNeighobrs) {
//				addLYSnapshotInfo(lySnapshotResult.getServentId(), lySnapshotResult);
//			}
			for (LYSnapshotResult lySnapshotResult : allResultsFromNeighbors.get(AppConfig.myServentInfo.getId())) {
				addLYSnapshotInfo(lySnapshotResult.getServentId(), lySnapshotResult);
			}

			//4 print
			int sum;
			sum = 0;
			for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
				sum += nodeResult.getValue().getRecordedAmount();
				AppConfig.timestampedStandardPrint(
						"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
			}
			for(int i = 0; i < AppConfig.getServentCount(); i++) {
				for (int j = 0; j < AppConfig.getServentCount(); j++) {
					if (i != j) {
						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
							int ijAmount = collectedLYValues.get(i).getGiveHistory().get(j);
							int jiAmount = collectedLYValues.get(j).getGetHistory().get(i);
							
							if (ijAmount != jiAmount) {
								String outputString = String.format(
										"Unreceived bitcake amount: %d from servent %d to servent %d",
										ijAmount - jiAmount, i, j);
								AppConfig.timestampedStandardPrint(outputString);
								sum += ijAmount - jiAmount;
							}
						}
					}
				}
			}
			
			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

			//5 notify everyone that snapshot has ended so that they can take suitable steps
			List<Integer> initiators = new ArrayList<>(gotResultsFrom);
			for (int child : AppConfig.treeChildren) {
				LYSKTreeNotifyMessage toSend = new LYSKTreeNotifyMessage(AppConfig.myServentInfo,
						AppConfig.getInfoById(child), initiators);

				MessageUtil.sendMessage(toSend);
			}

			//6 take suitable steps of my own
			synchronized (AppConfig.versionLock) {
				for (int initiator : initiators) {
					int oldVersion = AppConfig.initiatorVersions.get(initiator);
					AppConfig.initiatorVersions.put(initiator, oldVersion + 1);
				}

				((LaiYangBitcakeManager)bitcakeManager).setFromUncertainHistory();
				((LaiYangBitcakeManager)getBitcakeManager()).flushUncertainHistory();

				AppConfig.region.set(-1);
				AppConfig.treeParent.set(-1);
				AppConfig.treeChildren.clear();
			}

			collectedLYValues.clear(); //reset for next invocation
			collecting.set(false);
		}

	}
	
	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		collectedLYValues.put(id, lySnapshotResult);
	}

	public void setMyMap(Map<Integer, LYSnapshotResult> myMap) {
		this.myMap = myMap;
	}

	@Override
	public void startCollecting() {
		if (!AppConfig.myServentInfo.isInitiator()) {
			AppConfig.timestampedErrorPrint("Tried to collect snapshot from non-initiator node");
			return;
		}

		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

	public void checkPendingMessages() {
		Iterator<SKRoundResult> iterator = AppConfig.pendingResults.iterator();
		while (iterator.hasNext()) {
			SKRoundResult pendingMessage = iterator.next();

			if (pendingMessage.getRoundNumber() == expectingMessage.get(pendingMessage.getSender())) {
				AppConfig.regionResponses.add(pendingMessage);
				iterator.remove();
			}
		}
	}
}
