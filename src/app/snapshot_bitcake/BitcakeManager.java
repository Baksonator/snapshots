package app.snapshot_bitcake;

/**
 * Describes a bitcake manager. These classes will have the methods
 * for handling snapshot recording and sending info to a collector.
 * 
 * @author bmilojkovic
 *
 */
public interface BitcakeManager {

	void takeSomeBitcakes(int amount);
	void addSomeBitcakes(int amount);
	int getCurrentBitcakeAmount();
	
}
