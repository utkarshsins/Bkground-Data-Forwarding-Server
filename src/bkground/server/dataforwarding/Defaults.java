package bkground.server.terminal;

public class Defaults {

	public static final String TERMINAL_SERVER_BANNER = "#####################################\n"
			+ "Terminal Server\n" + "#####################################";

	public static int getDefaultListenerThreadCount() {
		return 1;
		// return Runtime.getRuntime().availableProcessors();
	}

	public static int getDefaultProcessorThreadCount() {
		return Runtime.getRuntime().availableProcessors();
	}

}
