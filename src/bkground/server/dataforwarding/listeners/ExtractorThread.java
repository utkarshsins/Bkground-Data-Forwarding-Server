package bkground.server.dataforwarding.listeners;

import bkground.server.dataforwarding.jdbc_connect;

public class ExtractorThread extends Thread {

	public jdbc_connect mysqlConnector;
	public ExtractorThread(Runnable arg0) {

		super(arg0);
	}
}