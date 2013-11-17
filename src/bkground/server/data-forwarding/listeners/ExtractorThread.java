package bkground.server.terminal.listeners;

import java.nio.ByteBuffer;

public class ExtractorThread extends Thread {

	public ByteBuffer buffer;

	public ExtractorThread(Runnable arg0) {

		super(arg0);

		buffer = ByteBuffer.allocate(1024);

	}

}
