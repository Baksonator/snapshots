package app;

import java.io.Serializable;
import java.util.List;

/**
 * This is an immutable class that holds all the information for a servent.
 *
 * @author bmilojkovic
 */
public class ServentInfo implements Serializable {

	private static final long serialVersionUID = 5304170042791281555L;
	private final int id;
	private final String ipAddress;
	private final int listenerPort;
	private final List<Integer> neighbors;
	
	public ServentInfo(String ipAddress, int id, int listenerPort, List<Integer> neighbors) {
		this.ipAddress = ipAddress;
		this.listenerPort = listenerPort;
		this.id = id;
		this.neighbors = neighbors;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public int getListenerPort() {
		return listenerPort;
	}

	public int getId() {
		return id;
	}
	
	public List<Integer> getNeighbors() {
		return neighbors;
	}
	
	@Override
	public String toString() {
		return "[" + id + "|" + ipAddress + "|" + listenerPort + "]";
	}
}
