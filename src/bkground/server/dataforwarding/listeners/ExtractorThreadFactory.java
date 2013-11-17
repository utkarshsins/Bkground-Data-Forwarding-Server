package bkground.server.dataforwarding.listeners;

import java.util.concurrent.ThreadFactory;

public class ExtractorThreadFactory implements ThreadFactory {

	@Override
	public Thread newThread(Runnable arg0) {

		return new ExtractorThread(arg0);

	}

}
