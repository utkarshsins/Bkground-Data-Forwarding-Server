package bkground.server.dataforwarding;

import java.util.ArrayList;
import java.util.List;

public class TerminalDataBase {

	public static final String BKGROUND = "bkground";

	public static final String AUTHENTICATION = "authentication";
	public static final String AUTHTYPE = "login";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";

	public static final String MESSAGE = "message";
	public static final String SUBSCRIPTIONID = "subscriptionid";
	public static final String MESSAGEBODY = "messagebody";

	public static enum STATE {
		NULL, BKGROUND, AUTHENTICATION, LOGIN, USERNAME, PASSWORD, MESSAGE, SUBSCRIPTIONID, MESSAGEBODY
	};

	public List<TerminalDataBase> content;

	public String body;

	public boolean open;

	public TerminalDataBase() {
		content = new ArrayList<TerminalDataBase>();
		body = null;
		open = true;
	}

	public boolean isSafeToClose() {
		boolean safe = open;

		for (TerminalDataBase contentItem : content) {
			safe = safe && contentItem.isClosed();
		}

		return safe;
	}

	public boolean isClosed() {
		return !open;
	}

	public boolean close() {
		if (isSafeToClose())
			open = false;

		return !open;
	}

	public TerminalDataBase getChild(int i) {
		try {
			return content.get(i);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public void addChild(TerminalDataBase data) {
		content.add(data);
	}

}
