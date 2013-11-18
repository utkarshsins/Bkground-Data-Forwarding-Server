package bkground.server.dataforwarding.listeners;

import java.util.List;
import java.util.Vector;

import bkground.server.dataforwarding.listeners.ListenerSocket.DataProcessingTask;

public class DataProcessingThread extends Thread {
	
	/*
	 * For every thread, in this data forwarding layer, We have to maintain the following:
	 * 
	 * XMLData: This will be the data that we got from the Terminal servers.
	 * 
	 * 
	 */	
	public String xmlData;
	
	private static final String SUBSCRIPTIONID_STRING = "<subscriptionID>";
	private static final String SUBSCRIPTIONID_STRING_END = "</subscriptionID>";
	private int subscriptionID;
	
	public DataProcessingThread (Runnable arg0) {
		super(arg0);
		xmlData = "Sample";
		System.out.println("DataProcessing .. " + xmlData);		
	}
	
	public void setTask(DataProcessingTask t) {
		xmlData = t.xmlData;
	}

	public void ExtractAndSend() {
		// Extract the subscription ID.
		this.subscriptionID = extractSubscriptionID();
		if (this.subscriptionID == -1) {
			System.err.println("Error: invalid xml stanxa ");
			return;
		}
		
		// Get users corresponding to this topic
		List <Integer> Users = findUsersForTopic();
		
		// Now we have the users, who are subscribed to this topic.
		// We now want to send this informaiton along with the xml data to the 
		// Terminal servers.
		System.out.println("Users for this topic are as follows: ");
		for (Integer i : Users) {
			System.out.print(i + " ");
		}
		
		System.out.println();
		
		// Now we need to append the userIDs to the xml Stanza.
		String newXMLData = appendUsersList(Users);
		
		System.out.println("\n\n" + newXMLData + "\n\n");
		System.out.println("Implement the sending method \n");
	}
	
	
	public int getSubscriptionID() {
		return this.subscriptionID;
	}
	public void setSubscriptionID(int ID) {
		this.subscriptionID = ID;
	}
	
	/*
	 * Append the list of users to the original message.
	 */ 
	public String appendUsersList (List <Integer> users) {
		String newXMLData = null;
		
		// Currently inserting another node in the xml 
		// Structure, after body.
		int bodyEnd = this.xmlData.indexOf("</body>") + "</body>".length();
		StringBuilder sb = new StringBuilder();
		sb.append(this.xmlData.substring(0, bodyEnd));
		sb.append("<users>");
		for (Integer i : users)
			sb.append(i + " ");
		sb.append("</users>");
		sb.append(this.xmlData.substring(bodyEnd, this.xmlData.length()));
		newXMLData = sb.toString();
		return newXMLData;
	}
	
	/*
	 * Get the subscription ID from the xml structure.
	 */
	private int extractSubscriptionID() {
		// Get the subscription ID from the xmlData String.
		int subscriptionID = -1;
		subscriptionID = Integer.parseInt(this.xmlData.substring(
				this.xmlData.indexOf(SUBSCRIPTIONID_STRING) + SUBSCRIPTIONID_STRING.length(), 
				this.xmlData.indexOf(SUBSCRIPTIONID_STRING_END)));

		if (subscriptionID == -1) {
			System.err.println("Defected xml stanza returned \n" + this.xmlData + "\n");
			return -1;
		}
		return subscriptionID;
	}
	/*
	 * This method is meant to do the database query corresponding to the 
	 * subscriptionID and return the list of indices of the users subsscribed to
	 * this topic.
	 */
	private List<Integer> findUsersForTopic() {
		List<Integer> Users = new Vector<Integer>();
		System.out.println("Getting Users for " + this.subscriptionID + "\n");
		// TODO 
		// Do the database query here
		Users.add(1);Users.add(2);Users.add(4);Users.add(8);Users.add(16);Users.add(32);
		Users.add(64);Users.add(128);Users.add(256);Users.add(512);Users.add(1024);Users.add(2014);
		return Users;
	}

	
}