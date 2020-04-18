package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is written in recordedAmount.
 * The channel messages are recorded in giveHistory and getHistory.
 * In Lai-Yang, the initiator has to reconcile the differences between
 * individual nodes, so we just let him know what we got and what we gave
 * and let him do the rest.
 * 
 * @author bmilojkovic
 *
 */
public class LYSnapshotResult implements Serializable {

	private static final long serialVersionUID = 8939516333227254439L;
	
	private final int serventId;
	private final int recordedAmount;
	private final Map<Integer, Integer> giveHistory;
	private final Map<Integer, Integer> getHistory;
	private Set<Integer> neighboringRegions;
	
	public LYSnapshotResult(int serventId, int recordedAmount,
			Map<Integer, Integer> giveHistory, Map<Integer, Integer> getHistory) {
		this.serventId = serventId;
		this.recordedAmount = recordedAmount;
		this.giveHistory = new ConcurrentHashMap<>(giveHistory);
		this.getHistory = new ConcurrentHashMap<>(getHistory);
	}
	public int getServentId() {
		return serventId;
	}
	public int getRecordedAmount() {
		return recordedAmount;
	}
	public Map<Integer, Integer> getGiveHistory() {
		return giveHistory;
	}
	public Map<Integer, Integer> getGetHistory() {
		return getHistory;
	}
	public Set<Integer> getNeighboringRegions() { return neighboringRegions; }
	public void setNeighboringRegions(Set<Integer> neighboringRegions) { this.neighboringRegions = neighboringRegions; }
}
