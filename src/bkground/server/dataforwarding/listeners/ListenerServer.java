package bkground.server.dataforwarding.listeners;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

import bkground.server.dataforwarding.ServerInfo;

/**
 * Thread that will listen for incoming connections
 */
public class ListenerServer extends Thread {

	public static final String THREAD_NAME = "THREAD_SERVER";

	private int port;
	private int listenerIterator;

	private ServerSocketChannel ssc;

	private ServerInfo serverInfo;

	private ConcurrentHashMap<Integer, ListenerSocket> listenerSockets;

	public ListenerServer() {
		super();
		setName(THREAD_NAME);

		this.port = ServerInfo.SERVER_INFO_DEFAULT_PORT;
		this.listenerIterator = -1;
	}

	public ListenerServer(
			ConcurrentHashMap<Integer, ListenerSocket> listenerSockets,
			ServerInfo serverInfo) {
		this();

		this.serverInfo = serverInfo;
		this.listenerSockets = listenerSockets;
	}

	/**
	 * Get the port on which the server is listening for new sockets.
	 * 
	 * @return int Port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the port on which the server will listen for new sockets.
	 * 
	 * @return {@link ListenerServer} The current instance of the ListenerServer
	 *         for chaining
	 * @throws IllegalAccessException
	 *             If listener server has already started
	 */
	public ListenerServer setPort(int port) throws IllegalAccessException {
		if (isAlive()) {
			throw new IllegalAccessException();
		}
		this.port = port;
		return this;
	}

	/**
	 * Start the server in its separate thread.
	 * 
	 * @throws IOException
	 *             if listener socket fails to setup
	 */
	public synchronized void startServer() throws IOException {
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(true);
		ssc.socket().bind(new InetSocketAddress(port));
		
		System.out.println("Server started on port " + port);
		
		super.start();
	}

	/**
	 * @deprecated call startServer() instead
	 */
	@Override
	public synchronized void start() {
		try {
			System.err.println("Warning : Using a deprecated function.");
			startServer();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void run() {
		while (true) {
			try {

				SocketChannel socketChannel = ssc.accept();

				System.out.println("Incoming socket from "
						+ socketChannel.getRemoteAddress());

				if (listenerSockets.values().size() != 0) {

					listenerIterator = (++listenerIterator)
							% listenerSockets.values().size();
					listenerSockets.get(listenerIterator).enqueSocketChannel(
							socketChannel);
					serverInfo.socketListenersMap.put(socketChannel,
							listenerSockets.get(listenerIterator));

				} else {

					System.err.println("No socket listener thread. Cleaning.");
					socketChannel.close();

				}

			} catch (AsynchronousCloseException e) {
				System.err.println("Asynchronous close on server listener");
				break;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
		System.err.println("Stopping Server Listener!!");
	}

	@Override
	public void interrupt() {
		if (ssc != null) {
			try {
				ssc.close();
			} catch (IOException e) {
				System.err.println("Error closing listener on interrupt");
				e.printStackTrace();
			}
		}
		super.interrupt();
	}

}
