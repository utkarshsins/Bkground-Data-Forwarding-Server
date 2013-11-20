package bkground.server.dataforwarding;

import java.util.concurrent.Callable;

import bkground.server.dataforwarding.listeners.DataProcessingThread;

public class DataProcessTask implements Callable<Boolean>{
	public DataProcessTaskEntry task;
	public class DataProcessTaskEntry {
		public ServerInfo serverInfo;
		public String subsID;
		public String msg;
		public DataProcessTaskEntry(ServerInfo serverInfo, String s, String m) {
			this.msg = m;
			this.subsID = s;
			this.serverInfo = serverInfo;
		}
		
	}
	public DataProcessTask(ServerInfo serverInfo, String subsID, String message) {
		this.task = new DataProcessTaskEntry(serverInfo, subsID, message);
	}

	@Override
	public Boolean call() throws Exception {
		DataProcessingThread thread = (DataProcessingThread) Thread.currentThread();			
		
		// Set task
		thread.setTask(task);
		
		// Lets start the work
		thread.ExtractAndSend();
		
		return null;
	}


}
