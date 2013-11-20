package bkground.server.dataforwarding;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import bkground.server.dataforwarding.listeners.DataProcessingThreadFactory;
import bkground.server.dataforwarding.listeners.ExtractorThreadFactory;
import bkground.server.dataforwarding.listeners.ListenerServer;
import bkground.server.dataforwarding.listeners.ListenerSocket;

public class ServerInfo {

	public static final int SERVER_INFO_DEFAULT_PORT = 4041;

	public ConcurrentHashMap<Integer, ListenerSocket> listenerSocketMap;
	
	public ConcurrentHashMap<SocketChannel, ListenerSocket> socketListenersMap; 

	/* I plan to use this. */
	public ConcurrentHashMap<Integer, SocketChannel> socketIDMap;
	public ConcurrentHashMap<Integer, SocketAddress> socketAddMap;
	public ListenerServer listenerServer;

	public ExecutorService socketProcessorPool;
	
	public ExecutorService dataProcessingPool;
	public static int id;
	public AsyncXMLInputFactory xmlInputFactory;
	public ServerInfo() {
		this.listenerSocketMap = new ConcurrentHashMap<Integer, ListenerSocket>();
		this.socketListenersMap = new ConcurrentHashMap<SocketChannel, ListenerSocket>();
		this.socketIDMap = new ConcurrentHashMap<Integer, SocketChannel>();
		this.socketAddMap = new ConcurrentHashMap<Integer, SocketAddress>();
		this.listenerServer = new ListenerServer(listenerSocketMap, this);
		this.socketProcessorPool = Executors.newFixedThreadPool(
				Defaults.getDefaultProcessorThreadCount(),
				new ExtractorThreadFactory());
		this.xmlInputFactory = new InputFactoryImpl();
		
		this.dataProcessingPool = Executors.newFixedThreadPool(
				Defaults.getDefaultDataProcessingThreadCount(), 
				new DataProcessingThreadFactory());
	}

	public void init() throws IOException {

		if (listenerSocketMap.size() == 0) {
			try {
				setThreadCount(Defaults.getDefaultListenerThreadCount());
			} catch (IllegalAccessException e) {
				System.out.println("Should not have happened. Halting.");
				e.printStackTrace();
				System.exit(-1);
			}
		}

		for (ListenerSocket listenerSocket : listenerSocketMap.values()) {
			listenerSocket.start();
		}

		// Must start after listenerSockets
		listenerServer.startServer();
	}

	/**
	 * Set the port on which the server will listen for new sockets.
	 * 
	 * @return {@link ServerInfo} The current instance of the ServerInfo for
	 *         chaining
	 * @throws IllegalAccessException
	 *             If listener server has already started
	 */
	public ServerInfo setPort(int port) throws IllegalAccessException {
		listenerServer.setPort(port);
		return this;
	}

	/**
	 * Set the number of listener socket threads.
	 * 
	 * @return {@link ServerInfo} The current instance of the ServerInfo for
	 *         chaining
	 * @throws IllegalAccessException
	 *             If thread count has been set once
	 */
	public ServerInfo setThreadCount(int count) throws IllegalAccessException {
		if (listenerSocketMap.size() > 0) {
			throw new IllegalAccessException();
		}

		for (int i = 0; i < count; i++) {
			try {
				ListenerSocket listenerSocket = new ListenerSocket(this, i);
				listenerSocketMap.put(i, listenerSocket);
			} catch (IOException e) {
				count--;
			}
		}

		System.out.println("Started " + count + " "
				+ ListenerSocket.THREAD_NAME);

		return this;
	}
	public ServerInfo setID(int id) {
		ServerInfo.id = id;
		return this;
	}
}