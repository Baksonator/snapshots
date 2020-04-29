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

			List<LYSnapshotResult> lySnapshotResults = new ArrayList<>();
			while (children > 0) {
				try {
					lySnapshotResults.addAll(AppConfig.childrenResponses.take());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				children--;
			}

			for (LYSnapshotResult lySnapshotResult : lySnapshotResults) {
				neighobringRegions.addAll(lySnapshotResult.getNeighboringRegions());
				addLYSnapshotInfo(lySnapshotResult.getServentId(), lySnapshotResult);
			}

			// Exchange, for now everyone sends to everyone
			neighobringRegions.remove(AppConfig.myServentInfo.getId());
			int expectedBlanks = neighobringRegions.size();
			Map<Integer, Set<Integer>> neighborHas = new HashMap<>();

			Map<Integer, Integer> sentMessageNumber = new HashMap<>();

			lySnapshotResults.add(collectedLYValues.get(AppConfig.myServentInfo.getId()));

			StringBuilder stringBuilder = new StringBuilder();
			String comma = "";
			for (Integer neighborInitiator : neighobringRegions) {
				// Start first round, send messages to all neighboring regions about the stuff we know
				// Update the neighborHas map where we track what our neighbor already has that we dont have to send
				// Update the maps for counting the number of messages received

				Set<Integer> sent = new HashSet<>();
				sent.add(AppConfig.myServentInfo.getId());
				neighborHas.put(neighborInitiator, sent);

				Map<Integer, List<LYSnapshotResult>> toSend = new HashMap<>();
				toSend.put(AppConfig.myServentInfo.getId(), lySnapshotResults);

				LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(AppConfig.myServentInfo,
						AppConfig.getInfoById(neighborInitiator), toSend, 1);

				sentMessageNumber.put(neighborInitiator, 1);
				expectingMessage.put(neighborInitiator, 1);

				stringBuilder.append(comma);
				stringBuilder.append(neighborInitiator);
				comma = ",";

				MessageUtil.sendMessage(lyskNeighborNotifyMessage);
			}

			AppConfig.timestampedStandardPrint("My neighbors are: " + stringBuilder.toString());

			Set<Integer> gotResultsFrom = new HashSet<>();

			List<LYSnapshotResult> resultsFromNeighobrs = new ArrayList<>();
			while (true) {
				int roundAnswers = expectedBlanks;
				int blankAnswers = expectedBlanks;

				Map<Integer, List<LYSnapshotResult>> newRegions = new HashMap<>();
				boolean gotNewResults = false;

				while (roundAnswers > 0) {

					while (AppConfig.regionResponses.isEmpty()) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						checkPendingMesasges();
					}

					try {
						SKRoundResult newResult = AppConfig.regionResponses.take();

						if (newResult.getLySnapshotResult().isEmpty()) {
							blankAnswers--;
							expectedBlanks--;
							AppConfig.timestampedStandardPrint("Got blank message from: " +  newResult.getSender() + " in round: " + newResult.getMessageNo());
						} else {

							StringBuilder sb = new StringBuilder();
							comma = "";

							for (Integer key : newResult.getLySnapshotResult().keySet()) {
								if (gotResultsFrom.add(key)) {
									newRegions.put(key, newResult.getLySnapshotResult().get(key));
									neighborHas.get(newResult.getSender()).add(key);
									gotNewResults = true;
									resultsFromNeighobrs.addAll(newResult.getLySnapshotResult().get(key));
								}
								neighborHas.get(newResult.getSender()).add(key);
								sb.append(comma);
								sb.append(key);
								comma = ",";
							}

							AppConfig.timestampedStandardPrint("Got results for regions: " + sb.toString() + " from: " + newResult.getSender() + " in round: " + newResult.getMessageNo());
						}

						roundAnswers--;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

				if (blankAnswers == 0) {
					break;
				}

				Set<Integer> toRemove = new HashSet<>();

				if (!gotNewResults) {
					for (Integer neighborInitiator : neighobringRegions) {
						LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(AppConfig.myServentInfo,
								AppConfig.getInfoById(neighborInitiator), new HashMap<>(), sentMessageNumber.get(neighborInitiator) + 1);

						toRemove.add(neighborInitiator);

						sentMessageNumber.put(neighborInitiator, sentMessageNumber.get(neighborInitiator) + 1);

						MessageUtil.sendMessage(lyskNeighborNotifyMessage);
					}
				} else {

					for (Integer neighborInitiator : neighobringRegions) {
						boolean shouldSend = false;
						Map<Integer, List<LYSnapshotResult>> toSend = new HashMap<>();

						for (Integer newRegion : newRegions.keySet()) {
							if (!neighborHas.get(neighborInitiator).contains(newRegion)) {
								shouldSend = true;

								toSend.put(newRegion, newRegions.get(newRegion));
							}
						}

						if (!shouldSend) {
							LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(AppConfig.myServentInfo,
									AppConfig.getInfoById(neighborInitiator), new HashMap<>(), sentMessageNumber.get(neighborInitiator) + 1);

							toRemove.add(neighborInitiator);

							sentMessageNumber.put(neighborInitiator, sentMessageNumber.get(neighborInitiator) + 1);

							MessageUtil.sendMessage(lyskNeighborNotifyMessage);
						} else {
							LYSKNeighborNotifyMessage lyskNeighborNotifyMessage = new LYSKNeighborNotifyMessage(AppConfig.myServentInfo,
									AppConfig.getInfoById(neighborInitiator), toSend, sentMessageNumber.get(neighborInitiator) + 1);

							sentMessageNumber.put(neighborInitiator, sentMessageNumber.get(neighborInitiator) + 1);

							MessageUtil.sendMessage(lyskNeighborNotifyMessage);
						}
					}
				}

				neighobringRegions.removeAll(toRemove);

				expectingMessage.replaceAll((k1, v) -> expectingMessage.get(k1) + 1);

			}

			for (LYSnapshotResult lySnapshotResult : resultsFromNeighobrs) {
				addLYSnapshotInfo(lySnapshotResult.getServentId(), lySnapshotResult);
			}

			//print
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

			List<Integer> initiators = new ArrayList<>(gotResultsFrom);
			for (int child : AppConfig.treeChildren) {
				LYSKTreeNotifyMessage toSend = new LYSKTreeNotifyMessage(AppConfig.myServentInfo, AppConfig.getInfoById(child),
						initiators);

				MessageUtil.sendMessage(toSend);
			}

			// TODO Mora i da se azurira i isprazni "nesigurna" istorija
			for (int initiator : initiators) {
				int oldVersion = AppConfig.initiatorVersions.get(initiator);
				AppConfig.initiatorVersions.put(initiator, oldVersion + 1);
			}

			AppConfig.region.set(-1);
			AppConfig.treeParent.set(-1);
			AppConfig.treeChildren.clear();

			collectedLYValues.clear(); //reset for next invocation
			collecting.set(false);
		}

	}
	
	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		collectedLYValues.put(id, lySnapshotResult);
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

	public void checkPendingMesasges() {

		Iterator<SKRoundResult> iterator = AppConfig.pendingResults.iterator();
		while (iterator.hasNext()) {
			SKRoundResult pendingMessage = iterator.next();

			if (pendingMessage.getMessageNo() == expectingMessage.get(pendingMessage.getSender())) {
				AppConfig.regionResponses.add(pendingMessage);
				iterator.remove();
				// Do this in round handler
//				expectingMessage.put(pendingMessage.getSender(), expectingMessage.get(pendingMessage.getSender() + 1));
			}
		}
	}
}
