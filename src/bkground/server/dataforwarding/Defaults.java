package bkground.server.dataforwarding;

public class Defaults {

	public static final String TERMINAL_SERVER_BANNER = "#####################################\n"
			+ "Terminal Server\n" + "#####################################";

	public static int getDefaultListenerThreadCount() {
		return 1;
		// return Runtime.getRuntime().availableProcessors();
	}

	public static int getDefaultDataProcessingThreadCount() {
		return 4;
		// return Runtime.getRuntime().availableProcessors();
	}
	public static String getDefaultDatabaseAddress() {
		return "192.168.43.5";
	}
	
	public static String getDefaultDatabaseName() {
		return "bkground_database";
	}
	
	public static String getDefaultDatabaseUsername() {
		return "admin";
	}
	
	public static String getDefaultDatabasePassword() {
		return "admin";
	}

	public static int getDefaultProcessorThreadCount() {
		return Runtime.getRuntime().availableProcessors();
	}

}
