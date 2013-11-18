package bkground.server.dataforwarding.listeners;

import java.util.concurrent.ThreadFactory;

public class DataProcessingThreadFactory implements ThreadFactory{

	@Override
	public Thread newThread(Runnable arg0) {

		return new DataProcessingThread(arg0);
	}
}


