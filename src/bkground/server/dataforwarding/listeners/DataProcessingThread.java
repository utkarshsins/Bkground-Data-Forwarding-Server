package bkground.server.dataforwarding.listeners;

import java.util.List;

import bkground.server.dataforwarding.listeners.ListenerSocket.DataProcessingTask;

public class DataProcessingThread extends Thread {
	
	/*
	 * For every thread, in this data forwarding layer, We have to maintain the following:
	 * 
	 * XMLData: This will be the data that we got from the Terminal servers.
	 * 
	 * 
	 */
	
	// public DataProcessingTask task;
	public String xmlData;
	public DataProcessingThread (Runnable arg0) {
		super(arg0);
		xmlData = "Sample";
		System.out.println("DataProcessing .. " + xmlData);		
	}
	
	public void setTask(DataProcessingTask t) {
		xmlData = t.xmlData;
	}

	/*
	 * This method is meant to do the database query corresponding to the 
	 * subscriptionID and return the list of indices of the users subsscribed to
	 * this topic.
	 */
	public List<Integer> findUsersForTopic(int subscriptionID) {
		List<Integer> Users = null;
		System.out.println("Getting Users for " + subscriptionID + "\n");
		// TODO 
		// Do the database query here.
		return Users;
	}

}