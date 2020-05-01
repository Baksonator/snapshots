package servent.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.SnapshotID;

/**
 * A default message implementation. This should cover most situations.
 * If you want to add stuff, remember to think about the modificator methods.
 * If you don't override the modificators, you might drop stuff.
 * @author bmilojkovic
 *
 */
public class BasicMessage implements Message {

	private static final long serialVersionUID = -9075856313609777945L;
	private final MessageType type;
	private final ServentInfo originalSenderInfo;
	private final ServentInfo receiverInfo;
	private final List<ServentInfo> routeList;
	private final String messageText;
	private final List<SnapshotID> snapshotIDS;
	private final boolean isUncertain;
	
	//This gives us a unique id - incremented in every natural constructor.
	private static AtomicInteger messageCounter = new AtomicInteger(0);
	private final int messageId;
	
	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.routeList = new ArrayList<>();
		this.messageText = "";
		this.snapshotIDS = new ArrayList<>();
		this.isUncertain = false;
		
		this.messageId = messageCounter.getAndIncrement();
	}
	
	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
			String messageText) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.routeList = new ArrayList<>();
		this.messageText = messageText;
		this.snapshotIDS = new ArrayList<>();
		this.isUncertain = false;
		
		this.messageId = messageCounter.getAndIncrement();
	}

//	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
//						String messageText, List<SnapshotID> snapshotIDS) {
//		this.type = type;
//		this.originalSenderInfo = originalSenderInfo;
//		this.receiverInfo = receiverInfo;
//		this.routeList = new ArrayList<>();
//		this.messageText = messageText;
//		this.snapshotIDS = snapshotIDS;
//
//		this.messageId = messageCounter.getAndIncrement();
//	}
	
	@Override
	public MessageType getMessageType() {
		return type;
	}

	@Override
	public ServentInfo getOriginalSenderInfo() {
		return originalSenderInfo;
	}

	@Override
	public ServentInfo getReceiverInfo() {
		return receiverInfo;
	}
	
	@Override
	public List<ServentInfo> getRoute() {
		return routeList;
	}
	
	@Override
	public String getMessageText() {
		return messageText;
	}

	@Override
	public List<SnapshotID> getSnapshotIDS() { return snapshotIDS; }

	@Override
	public boolean getUncertainty() { return isUncertain; }

	@Override
	public int getMessageId() {
		return messageId;
	}
	
	protected BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
						   List<ServentInfo> routeList, String messageText, List<SnapshotID> snapshotIDS, int messageId) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.routeList = routeList;
		this.messageText = messageText;
		this.snapshotIDS = snapshotIDS;
		this.isUncertain = false;

		this.messageId = messageId;
	}

	protected BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
						   List<ServentInfo> routeList, String messageText, List<SnapshotID> snapshotIDS, int messageId,
						   boolean isUncertain) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.routeList = routeList;
		this.messageText = messageText;
		this.snapshotIDS = snapshotIDS;
		this.isUncertain = isUncertain;

		this.messageId = messageId;
	}
	
	/**
	 * Used when resending a message. It will not change the original owner
	 * (so equality is not affected), but will add us to the route list, so
	 * message path can be retraced later.
	 */
	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;
		
		List<ServentInfo> newRouteList = new ArrayList<>(routeList);
		newRouteList.add(newRouteItem);

		return new BasicMessage(getMessageType(), getOriginalSenderInfo(),
				getReceiverInfo(), newRouteList, getMessageText(), getSnapshotIDS(), getMessageId());
	}
	
	/**
	 * Change the message received based on ID. The receiver has to be our neighbor.
	 * Use this when you want to send a message to multiple neighbors, or when resending.
	 */
	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			return new BasicMessage(getMessageType(), getOriginalSenderInfo(),
					newReceiverInfo, getRoute(), getMessageText(), getSnapshotIDS(), getMessageId());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
			
			return null;
		}
		
	}

	@Override
	public Message setSnapshotIDS() {
		List<SnapshotID> snapshotIDS = AppConfig.getSnapshotIDS();

		return new BasicMessage(getMessageType(), getOriginalSenderInfo(),
				getReceiverInfo(), getRoute(), getMessageText(), snapshotIDS, getMessageId());
	}

	@Override
	public Message setUncertainty() {
		boolean isUncertain;

		isUncertain = AppConfig.region.get() != -1;

		return new BasicMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(), getRoute(),
				getMessageText(), getSnapshotIDS(), getMessageId(), isUncertain);
	}

	/**
	 * Comparing messages is based on their unique id and the original sender id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BasicMessage) {
			BasicMessage other = (BasicMessage)obj;

			return getMessageId() == other.getMessageId() &&
					getOriginalSenderInfo().getId() == other.getOriginalSenderInfo().getId();
		}
		
		return false;
	}
	
	/**
	 * Hash needs to mirror equals, especially if we are gonna keep this object
	 * in a set or a map. So, this is based on message id and original sender id also.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getOriginalSenderInfo().getId());
	}
	
	/**
	 * Returns the message in the format: <code>[sender_id|message_id|text|type|receiver_id]</code>
	 */
	@Override
	public String toString() {
		return "[" + getOriginalSenderInfo().getId() + "|" + getMessageId() + "|" +
					getMessageText() + "|" + getMessageType() + "|" +
					getReceiverInfo().getId() + "]";
	}

	/**
	 * Empty implementation, which will be suitable for most messages.
	 */
	@Override
	public void sendEffect() {
		
	}
}
