package bkground.server.dataforwarding.listeners;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import bkground.server.dataforwarding.DataProcessTask.DataProcessTaskEntry;
import bkground.server.dataforwarding.ServerInfo;

public class DataProcessingThread extends Thread {
	
	/*
	 * For every thread, in this data forwarding layer, We have to maintain the following:
	 * 
	 * XMLData: This will be the data that we got from the Terminal servers.
	 * 
	 * 
	 */	
	public String msg;
	public String subsID;
	private String parentXML;
	ConcurrentHashMap<Integer, Integer> UserTerminalMap;
	ConcurrentHashMap<Integer, String> terminalReplyMap;
	List<Integer> TerminalID;
	private int subscriptionID;
	private ServerInfo serverInfo;
	public DataProcessingThread (Runnable arg0) {
		super(arg0);
		
		terminalReplyMap = new ConcurrentHashMap<>();
		UserTerminalMap = new ConcurrentHashMap<Integer, Integer>();
		TerminalID = new Vector<Integer>();
	}
	
	public void setTask(DataProcessTaskEntry t) {
		this.msg = t.msg;
		this.subsID = t.subsID; 
		this.serverInfo = t.serverInfo;
		this.parentXML = "<bkground><message><subscriptionid>" + this.subsID + 
				"/subscriptionid><messagebody>" + this.msg 
				+ "/messagebody></message></bkground>";
	}

	public void ExtractAndSend() {
		// Extract the subscription ID.
		this.subscriptionID = extractSubscriptionID();
		if (this.subscriptionID == -1) {
			System.err.println("Error: invalid xml stanxa ");
			return;
		}
		
		// Get users corresponding to this topic
		// TODO handle NULL exception.
		List <Integer> Users = findUsersForTopic();
		
		// Now we have the users, who are subscribed to this topic.
		// We now want to send this informaiton along with the xml data to the 
		// Terminal servers.
		System.out.println("Users for this topic are as follows: ");
		for (Integer i : Users) {
			System.out.print(i + " ");
		}
		
		System.out.println();
		

		// Now we need to know which user is connected to which terminal server.
		fillUserTerminalMap(Users);
		
		System.out.println("Terminal IDs");
		for (Integer i : TerminalID) 
			System.out.print(i+ " ");
		
		
		fillTerminalReplyMap(Users);
		System.out.println("Replies are");
		for (Integer i : TerminalID)
			System.out.println(i + " " + terminalReplyMap.get(i));

		sendAllTheReplies();
		System.out.println("Implement database connections");
	}
	
	/* 
	 * Send the message to all the terminal servers. 
	 * 
	 */
	private void sendAllTheReplies() {
		System.out.println("Sending all the replies ");
		for(Integer i: TerminalID) {
			String toSend = terminalReplyMap.get(i);
			if (toSend.isEmpty()) {
				System.out.println("Nothing to send, shouldn't happen. " + i);
				continue;
			}
			
			/* Get the socketChannel from the serverInfo */
			SocketChannel sc = null;
			InetSocketAddress remoteAddress = 
					(InetSocketAddress) this.serverInfo.socketAddMap.get(i);
			if (remoteAddress == null) {
				System.out.println("Terminal Server " 
						+ i + " is not present at the moment, skipping");
				continue;
			}
			ByteBuffer buffer = ByteBuffer.wrap(toSend.getBytes());
			try {
				System.out.println("opening socketChannel to " + remoteAddress.toString());
				sc = SocketChannel.open();
	            sc.configureBlocking(false);
	            sc.connect(remoteAddress);
	            while (!sc.finishConnect()) {
	                // pretend to do something useful here
	                System.out.println("Connecting ...");
	                try {
	                    Thread.sleep(3000);
	                } catch (InterruptedException e) {
	                    e.printStackTrace();
	                }
	            }
	            sc.write(buffer);
			} catch (IOException e1) {
				System.out.println("Problem while sending message to terminal server");
				e1.printStackTrace();
			}
		}
		
	}

	/*
	 * This method maps the terminalID to the reply that is needed 
	 * to be sent to that terminal.
	 */
	private void fillTerminalReplyMap(List<Integer> userList) {
		String toAdd = null;
		for (Integer i : userList) {
			if (terminalReplyMap.containsKey((UserTerminalMap.get(i)))) {
				toAdd = terminalReplyMap.get(UserTerminalMap.get(i)) + "<recipient>" + i +"</recipient>";	
				terminalReplyMap.put(UserTerminalMap.get(i),toAdd);
				toAdd = null;
			}
			else
				terminalReplyMap.put(UserTerminalMap.get(i),"<recipient>" + i +"</recipient>");				
		}
		
		toAdd = null;
		for (Integer i : TerminalID) {
			if (!terminalReplyMap.containsKey(i)) {
				System.out.println("Terminal ID and replies not consistent " + i);
				return ;
			}
			toAdd = appendUsersList(i);
			terminalReplyMap.replace(i, toAdd);
		}
	}

	/*
	 * Map the user with the terminalID to which they are connected.
	 */
	private void fillUserTerminalMap(List<Integer> userList) {
		for(Integer i : userList) {
			int t = getTerminalForUser(i);
			if (!TerminalID.contains(t))
				TerminalID.add(t);
			UserTerminalMap.put(i, t);
		}
	}
	
	
	/*
	 * Return the terminalID to which user is connected
	 */
	private int getTerminalForUser(Integer user) {
		int terminalID = user;
		//TODO do the database query here.
		return terminalID%3;
		
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
	public String appendUsersList (Integer terminal) {
		// TODO Remove this. This is bad.
		int messageStart = this.parentXML.indexOf("<message>") + "<message>".length();
		StringBuilder sb = new StringBuilder();
		sb.append(this.parentXML.substring(0, messageStart));
		sb.append(terminalReplyMap.get(terminal));
		sb.append(this.parentXML.substring(messageStart, this.parentXML.length()));
		return sb.toString();
	}
	
	/*
	 * Get the subscription ID from the xml structure.
	 */
	private int extractSubscriptionID() {
		// Get the subscription ID.
		return Integer.parseInt(this.subsID);
	}
	/*
	 * This method is meant to do the database query corresponding to the 
	 * subscriptionID and return the list of indices of the users subsscribed to
	 * this topic.
	 */
	private List<Integer> findUsersForTopic() {
		List<Integer> Users = new Vector<Integer>();

		// TODO Do the database query here
		// and put the results in the Users vector.
		Users.add(1);Users.add(2);
		Users.add(4);Users.add(3);
		Users.add(5);Users.add(6);
		return Users;
	}

	
}