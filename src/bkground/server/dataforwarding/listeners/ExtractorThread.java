package bkground.server.dataforwarding.listeners;

import java.nio.ByteBuffer;

import bkground.server.dataforwarding.listeners.ListenerSocket.DataProcessingTask;

public class ExtractorThread extends Thread {

	public ByteBuffer buffer;

	public ExtractorThread(Runnable arg0) {

		super(arg0);

		buffer = ByteBuffer.allocate(1024);

	}
	
	public void processXML(DataProcessingTask task) {		
		
	}

}