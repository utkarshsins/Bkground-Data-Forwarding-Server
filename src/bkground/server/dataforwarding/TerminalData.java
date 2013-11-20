package bkground.server.dataforwarding;

public class TerminalData extends TerminalDataBase {

	public STATE state;
	public TerminalData() {
		state = STATE.BKGROUND;
	}
	
	@Override
	public boolean close() {
		boolean closed = super.close();
		
		if(closed)
			state = STATE.NULL;
		
		return closed;
	}

	public static class Authentication extends TerminalDataBase {
		
		public Authentication(SocketInfo socketInfo) {
			socketInfo.data.state = STATE.AUTHENTICATION;
		}

		public static class Username extends TerminalDataBase {
			public Username(SocketInfo socketInfo) {
				body = new String();
				socketInfo.data.state = STATE.USERNAME;
			}
		}
		
		public static class Password extends TerminalDataBase {
			public Password(SocketInfo socketInfo) {
				body = new String();
				socketInfo.data.state = STATE.PASSWORD;
			}
		}

	}

	public static class Message extends TerminalDataBase {

		public static class SubscriptionID extends TerminalDataBase {
			public SubscriptionID(SocketInfo socketInfo) {
				body = new String();
				socketInfo.data.state = STATE.SUBSCRIPTIONID;
			}
		}
		
		public static class MessageBody extends TerminalDataBase {
			public MessageBody(SocketInfo socketInfo) {
				body = new String();
				socketInfo.data.state = STATE.MESSAGEBODY;
			}
		}

	}

}
