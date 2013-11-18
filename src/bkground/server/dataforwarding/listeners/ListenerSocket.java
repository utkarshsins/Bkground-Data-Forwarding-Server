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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import bkground.server.dataforwarding.ServerInfo;

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
				removeSocketChannel(pendingSocketsRemove.remove(0));

			try {

				selected = selector.select();
				System.out.println("Selected " + selected);

				// TODO
				// Code to process data from selector's selected keys
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				List<Future<Boolean>> processedFutures = new ArrayList<Future<Boolean>>();

				for (SelectionKey readyKey : readyKeys) {

					if (readyKey.isReadable()) {
						processedFutures.add(serverInfo.socketProcessorPool
								.submit(new RegisterReadTask(readyKey)));
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
		socketChannel.register(selector, SelectionKey.OP_READ);

		System.out.println("Registered socket to " + getName());

		return true;
	}

	/**
	 * This function will unregister a closed socket request registered with
	 * this data-listener thread.
	 * 
	 * @param
	 * @return boolean true if removal was successful
	 */
	private boolean removeSocketChannel(SelectionKey key) {

		key.cancel();
		serverInfo.socketListenersMap.remove(key.channel());

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

	private class RegisterReadTask implements Callable<Boolean> {
		SelectionKey key;

		public RegisterReadTask(SelectionKey readyKey) {
			this.key = readyKey;
		}

		@Override
		public Boolean call() throws Exception {

			// TODO
			// Extract data from socket into a data structure here and return
			// true.

			SocketChannel channel = (SocketChannel) key.channel();

			// Keep one buffer in each thread always ready. Don't create a new
			// buffer for every event.
			ExtractorThread thread = (ExtractorThread) Thread.currentThread();
			ByteBuffer buffer = thread.buffer;
			int bytesRead = channel.read(buffer);
			while (bytesRead > 0) {

				System.out.print("Read : ");
				buffer.flip();

				while (buffer.hasRemaining()) {
					System.out.print((char) buffer.get());
				}

				buffer.clear();
				bytesRead = channel.read(buffer);
			}
			System.out.println();

			// ------------------------------------------------------------------------------------------------
			// TODO By this point, the xml data should have been completely 
			// inserted in the threadbuffer.
			
			// Add the test to the data processing pool.
			// String xmlData = thread.buffer.toString();
			String xmlData = "<?xml version=\"1.0\"?><bkgroud><subscriptionID>1</subscriptionID><from>Publisher name Jani</from><body>Bloody message </body></bkgroud>";
	
			System.out.println("Adding task for " + xmlData);
			DataProcessingTask task = new DataProcessingTask(xmlData);
			
			// Adding the data to the thread pool.
			serverInfo.dataProcessingPool.submit(new RegisterDataProcessingTask(task));
			System.out.println("Task added \n");

			if (bytesRead == -1) {

				System.out.println("Socket closed. EOS. "
						+ "Unregister selector and remove associated data.");

				enqueSocketRemoval(key);

			}
			return true;
		}
	}
	
	
	/*
	 * Class for each task that is needed to be processed by the data proessing threads.
	 */
	public class DataProcessingTask {
		public String xmlData;
		public DataProcessingTask(String s) {
			xmlData = s;
		}
	}
	
	/*
	 * Class for registering the task to the data processing thread pool
	 */
	public class RegisterDataProcessingTask implements Callable<Boolean> {
		DataProcessingTask task;
		public static final String SUBSCRIPTIONID_STRING = "<subscriptionID>";
		public static final String SUBSCRIPTIONID_STRING_END = "</subscriptionID>";
		public RegisterDataProcessingTask(DataProcessingTask t) {
			this.task = t;
		}
		/*
		 * We need to do the process each of the submitted task in this thread.
		 * Processing involved doing the database query to know the users 
		 * correspoding to this subscription.
		 */		
		@Override
		public Boolean call() throws Exception {
			String data = this.task.xmlData;

			// Get the subscription ID from the xmlData String.
			int subscriptionID = -1;
			subscriptionID = Integer.parseInt(data.substring(
					data.indexOf(SUBSCRIPTIONID_STRING) + SUBSCRIPTIONID_STRING.length(), 
				 	data.indexOf(SUBSCRIPTIONID_STRING_END)));
			System.out.println("subscriptionID: " + subscriptionID);
			
			if (subscriptionID == -1) {
				System.err.println("Defected xml stanza returned \n" + data + "\n");
				return null;
			}
			
			DataProcessingThread thread = (DataProcessingThread) Thread.currentThread();
			
			// Set task
			thread.setTask(task);
			
			// Do the database query for this subscriptionID
			List<Integer> Users = thread.findUsersForTopic(subscriptionID);
			
			// Now we have the users, who are subscribed to this topic.
			// We now want to send this informaiton along with the xml data to the 
			// Terminal servers.
			
			System.out.println("Implement the sending method \n");
			return null;
		}
	}

}