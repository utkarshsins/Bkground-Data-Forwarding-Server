package bkground.server.dataforwarding.listeners;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.stream.XMLStreamException;

import bkground.server.dataforwarding.ServerInfo;
import bkground.server.dataforwarding.SocketInfo;
import bkground.server.dataforwarding.StreamProcessTask;

public class ListenerSocket extends Thread {

	public static final String THREAD_NAME = "THREAD_SOCKET";

	/**
	 * This is the selector that sockets for this data listener pool will wait
	 * on. Any incoming data from a socket will continue the blocked loop in
	 * {@link #run() run()}
	 */
	private Selector selector;

	private ServerInfo serverInfo;

	private List<SocketChannel> pendingSocketsAdd;
	private List<SelectionKey> pendingSocketsRemove;

	private ByteBuffer buffer;
	/**
	 * TODO Use this hash map to store information about socket channels that
	 * are registered on this server listener thread. Replace Integer with
	 * whatever key you want to use (possibly user-id), and replace
	 * SocketChannel with an object of a new class that contains SocketChannel
	 * as well as other relevant information needed about the Socket (like the
	 * affiliated user-details, status-of-socket, etc).
	 */
	// private ConcurrentHashMap<Integer, SocketChannel> socketChannelsMap;

	/**
	 * Constructor that will create a new selector on which sockets added to
	 * this thread through {@link #addSocketChannel(SocketChannel)} will be
	 * registered to listen for any incoming data.
	 * 
	 * @param i
	 * @throws IOException
	 */
	public ListenerSocket(ServerInfo serverInfo, int i) throws IOException {
		super();
		setName(THREAD_NAME + "_" + i);

		this.serverInfo = serverInfo;

		this.selector = Selector.open();

		// TODO
		// Check if this actually makes it thread safe. Elements are added to
		// pending sockets from another thread and removed from the current
		// thread.
		this.pendingSocketsAdd = Collections
				.synchronizedList(new ArrayList<SocketChannel>());
		this.pendingSocketsRemove = Collections
				.synchronizedList(new ArrayList<SelectionKey>());
		
		buffer = ByteBuffer.allocate(1024);
	}

	/**
	 * Get the selector that this thread will block on.
	 * 
	 * @return {@link Selector} The selector that this thread blocks on (Null if
	 *         no selector is set)
	 */
	public Selector getSelector() {
		return selector;
	}

	/**
	 * This is the executor function of the socket selector loop. The function
	 * blocks on select() function and waits until there are some sockets with
	 * data ready to be read
	 */
	@Override
	public void run() {
		int selected = 0;
		while (selector.isOpen()) {

			// Register newly created sockets with the current thread's listener
			while (pendingSocketsAdd.size() > 0) {

				try {
					addSocketChannel(pendingSocketsAdd.remove(0));
				} catch (ClosedChannelException e) {
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			// Remove sockets whose connections are closed (EOS recieved)
			while (pendingSocketsRemove.size() > 0)
				try {
					removeSocketChannel(pendingSocketsRemove.remove(0));
				} catch (IOException e1) {
					e1.printStackTrace();
				}					

			try {

				selected = selector.select();
				System.out.println("Selected " + selected);

				// TODO
				// Code to process data from selector's selected keys
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				List<Future<Boolean>> processedFutures = new ArrayList<Future<Boolean>>();

				for (SelectionKey readyKey : readyKeys) {

					if (readyKey.isReadable()) {
						processSelectionKey(readyKey);
					} else {
						System.err.println("Not readable. Something else. "
								+ "Handle this case.");
					}

				}

				for (Future<Boolean> future : processedFutures) {
					try {
						// boolean futureResult = future.get();
						future.get();
					} catch (InterruptedException | ExecutionException e) {
						System.err.println("Something went wrong when trying "
								+ "to wait for registering an incoming "
								+ "data-request. Check the processing "
								+ "in RegisterReadTask class.");
						e.printStackTrace();
					}
				}

				readyKeys.clear();

				System.out.println();

			} catch (IOException e) {
				System.err.println("select() failed for " + getName());
				e.printStackTrace();
			}
		}
	}
	/**
	 * This process is not thread pooled right now because incoming traffic is
	 * assumed to be handleable which is not an unfair assumption. There is no
	 * chat server to be implemented right now. Even if there was, check and be
	 * sure that you need to thread pool this module to gain some performance
	 * improvements. Otherwise keep it simple. Don't create four thread pools,
	 * if your work can be done using three only.
	 * 
	 * @param SelectionKey
	 *            key
	 */
	private void processSelectionKey(SelectionKey key) {

		// TODO
		// Extract data from socket into a data structure here and return
		// true.

		SocketChannel channel = (SocketChannel) key.channel();
		SocketInfo socketInfo = (SocketInfo) key.attachment();
		// TerminalSocketStream stream = socketInfo.socketStream;

		// Don't create a new buffer for every event.
		buffer.clear();
		int bytesRead = readBytesFromChannel(buffer, channel);

		while (bytesRead > 0) {

			buffer.flip();

			try {
				socketInfo.xmlFeeder.feedInput(buffer.array(),
						buffer.arrayOffset(), buffer.remaining());
				// buffer.position(newPosition)
			} catch (XMLStreamException e) {
				e.printStackTrace();
			}

			buffer.clear();

			bytesRead = readBytesFromChannel(buffer, channel);
			System.out.println("Registered streamprocess task ");
			serverInfo.socketProcessorPool.submit(new StreamProcessTask(key));

		}
		System.out.println();

		if (bytesRead == -1) {

			System.out.println("Socket closed. EOS. "
					+ "Unregister selector and remove associated data.");

			enqueSocketRemoval(key);

		}

	}


	private int readBytesFromChannel(ByteBuffer buffer, SocketChannel channel) {

		try {

			return channel.read(buffer);

		} catch (IOException e) {

			System.err.println("IOException when reading from channel. "
					+ "Closing socket.");
			e.printStackTrace();

			return -1;

		}

	}
	
	@Override
	public void interrupt() {
		try {
			selector.close();
		} catch (IOException e) {
			System.err.println("Error closing selector for " + getName());
			e.printStackTrace();
		}
		super.interrupt();
	}

	/**
	 * This function will register a new incoming socket request with this
	 * data-listener thread. Any data that will come on this thread will then be
	 * forwarded for extraction from the NIO APIs.
	 * 
	 * @param
	 * @return boolean true if registration was successful
	 * @throws ClosedChannelException
	 * @throws IOException
	 */
	private boolean addSocketChannel(SocketChannel socketChannel)
			throws ClosedChannelException, IOException {

		socketChannel.configureBlocking(false);
		SocketInfo socketInfo = new SocketInfo(serverInfo);
		socketInfo.socketChannel = socketChannel;
		socketInfo.readKey = (SelectionKey) socketChannel.register(selector, 
				SelectionKey.OP_READ); 
		socketInfo.readKey.attach(socketInfo);
		serverInfo.socketIDMap.put(socketInfo.socketID, socketInfo.socketChannel);
		serverInfo.socketAddMap.put(socketInfo.socketID, socketChannel.getRemoteAddress());
		System.out.println("Registered socket to " + getName() + " with ID " + 
					socketInfo.socketID + "  " + socketChannel.getRemoteAddress());

		return true;
	}

	/**
	 * This function will unregister a closed socket request registered with
	 * this data-listener thread.
	 * 
	 * @param
	 * @return boolean true if removal was successful
	 */
	private boolean removeSocketChannel(SelectionKey key) throws IOException{

		key.cancel();
		serverInfo.socketListenersMap.remove(key.channel());
		key.channel().close();
		return true;
	}

	public synchronized boolean enqueSocketRemoval(SelectionKey key) {

		pendingSocketsRemove.add(key);
		selector.wakeup();

		return true;
	}

	public synchronized boolean enqueSocketChannel(SocketChannel socketChannel) {

		pendingSocketsAdd.add(socketChannel);
		selector.wakeup();

		return true;
	}

	}