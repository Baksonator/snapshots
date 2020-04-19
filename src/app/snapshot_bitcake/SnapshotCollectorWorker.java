package app.snapshot_bitcake;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;

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
				addLYSnapshotInfo(lySnapshotResult.getServentId(), lySnapshotResult);
			}

			// TODO citati susedne regione iz lySnapshotResults
			// Takodje, ispis ce biti drugaciji, jer na pocetku nece imati potpune informacije
			// Kasnije mora da se implementira razmena po rundama
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

	@Override
	public int getMySnapshotVersion() {
		return mySnapshotVersion;
	}
}
