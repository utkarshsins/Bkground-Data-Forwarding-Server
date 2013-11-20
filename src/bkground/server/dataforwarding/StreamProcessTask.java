package bkground.server.dataforwarding;

import java.nio.channels.SelectionKey;
import java.util.concurrent.Callable;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import bkground.server.dataforwarding.TerminalDataBase.STATE;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class StreamProcessTask implements Callable<Object> {

	SelectionKey key;

	public StreamProcessTask(SelectionKey streamKey) {
		this.key = streamKey;
	}
	
	@Override
	public Object call() throws Exception {

		SocketInfo socketInfo = (SocketInfo) key.attachment();

		synchronized (socketInfo) {

			while ((socketInfo.xmlStreamReader.next()) != AsyncXMLStreamReader.EVENT_INCOMPLETE) {
				// printEvent(socketInfo.xmlStreamReader);
				processEvent(socketInfo);

				if (socketInfo.data != null)
					if (socketInfo.data.isClosed()) {
						processData(socketInfo);
						socketInfo.refreshReader();
					}

			}

		}

		return null;

	}

	private void processData(SocketInfo socketInfo) {

		TerminalDataBase data = socketInfo.data.getChild(0);
		if (data instanceof TerminalData.Message) {
			try {
				System.out.println("Task added" + data.getChild(0).body + " " + data.getChild(1).body);
				socketInfo.serverInfo.dataProcessingPool.submit(new DataProcessTask(socketInfo.serverInfo, 
						data.getChild(0).body, data.getChild(1).body));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else 
			System.out.println("Please handle this case of stream-ed data");

	}

	private void processEvent(SocketInfo socketInfo) {

		XMLStreamReader xmlr = socketInfo.xmlStreamReader;

		switch (xmlr.getEventType()) {

		case XMLStreamConstants.START_ELEMENT:
			processElementStart(socketInfo);
			break;

		case XMLStreamConstants.END_ELEMENT:
			processElementEnd(socketInfo);
			break;

		case XMLStreamConstants.CHARACTERS:
			processElementCharacters(socketInfo);
			break;
		}
	}

	private void processElementStart(SocketInfo socketInfo) {

		XMLStreamReader xmlr = socketInfo.xmlStreamReader;

		// Ordering has to be done in breadth first order. Otherwise it would be
		// difficult to maintain states.
		if (xmlr.hasName()) {
			String name = xmlr.getLocalName();
			// DEPTH-0
			if (name.equals(TerminalData.BKGROUND)) {
				socketInfo.data = new TerminalData();
				return;
			}
			// DEPTH-0 done. Now DEPTH-0 object should not be null.
			else if (socketInfo.data == null)
				;
			// DEPTH-1
			else {
				TerminalData terminalData1 = socketInfo.data;
				if (name.equals(TerminalData.AUTHENTICATION))
					if (terminalData1.getChild(0) == null) {
						terminalData1.addChild(new TerminalData.Authentication(
								socketInfo));
						return;
					} else
						;
				else if (name.equals(TerminalData.MESSAGE))
					if (terminalData1.getChild(0) == null) {
						terminalData1.addChild(new TerminalData.Message());
						return;
					} else
						;
				// DEPTH-1 done. Now DEPTH-1 object should not be null.
				else if (terminalData1.getChild(0) == null)
					;

				// DEPTH-2
				else {
					TerminalDataBase terminalData2 = terminalData1.getChild(0);
					if (terminalData2 instanceof TerminalData.Authentication) {
						if (name.equals(TerminalData.USERNAME))
							if (terminalData2.getChild(0) == null) {
								terminalData2
										.addChild(new TerminalData.Authentication.Username(
												socketInfo));
								return;
							} else
								;
						else if (name.equals(TerminalData.PASSWORD)) {
							if (terminalData2.getChild(1) == null) {
								terminalData2
										.addChild(new TerminalData.Authentication.Password(
												socketInfo));
								return;
							} else
								;
						}
						;
					} else if (terminalData2 instanceof TerminalData.Message)
						if (name.equals(TerminalData.SUBSCRIPTIONID))
							if (terminalData2.getChild(0) == null) {
								terminalData2
										.addChild(new TerminalData.Message.SubscriptionID(
												socketInfo));
								return;
							} else
								;
						else if (name.equals(TerminalData.MESSAGEBODY))
							if (terminalData2.getChild(1) == null) {
								terminalData2
										.addChild(new TerminalData.Message.MessageBody(
												socketInfo));
								return;
							} else
								;
						else
							;
					else
						;
				}

			}

			System.err.println("Illegal Start (IGNORING) : " + name);

		}

	}

	private void processElementCharacters(SocketInfo socketInfo) {

		XMLStreamReader xmlr = socketInfo.xmlStreamReader;

		String data = new String(xmlr.getTextCharacters(), xmlr.getTextStart(),
				xmlr.getTextLength());

		if (socketInfo.data != null) {
			if ((socketInfo.data.state == STATE.USERNAME || socketInfo.data.state == STATE.SUBSCRIPTIONID)
					&& socketInfo.data.getChild(0).getChild(0).open) {
				socketInfo.data.getChild(0).getChild(0).body += data;
				return;
			} else if ((socketInfo.data.state == STATE.PASSWORD || socketInfo.data.state == STATE.MESSAGEBODY)
					&& socketInfo.data.getChild(0).getChild(1).open) {
				socketInfo.data.getChild(0).getChild(1).body += data;
				return;
			}

		}

		System.err.println("Illegal characters (IGNORING) : " + data);

	}

	private void processElementEnd(SocketInfo socketInfo) {

		XMLStreamReader xmlr = socketInfo.xmlStreamReader;

		// Ordering has to be done in breadth first order. Otherwise it would be
		// difficult to maintain states.
		if (xmlr.hasName()) {
			String name = xmlr.getLocalName();
			// DEPTH-0
			if (name.equals(TerminalData.BKGROUND))
				if (socketInfo.data != null)
					if (socketInfo.data.close())
						return;
					else
						;
				else
					;
			else if (socketInfo.data == null)
				;
			// DEPTH-1
			else {
				TerminalData terminalData1 = socketInfo.data;
				if (name.equals(TerminalData.AUTHENTICATION))
					if (terminalData1.getChild(0) instanceof TerminalData.Authentication)
						if (terminalData1.getChild(0).close())
							return;
						else
							;
					else
						;
				else if (name.equals(TerminalData.MESSAGE))
					if (terminalData1.getChild(0) instanceof TerminalData.Message)
						if (terminalData1.getChild(0).close())
							return;
						else
							;
					else
						;
				// DEPTH-1 done. Now DEPTH-1 object should not be null.
				else if (terminalData1.getChild(0) == null)
					;
				// DEPTH-2
				else {
					TerminalDataBase terminalData2 = terminalData1.getChild(0);
					if (terminalData2 instanceof TerminalData.Authentication)
						if (name.equals(TerminalData.USERNAME))
							if (terminalData2.getChild(0) instanceof TerminalData.Authentication.Username)
								if (terminalData2.getChild(0).close())
									return;
								else
									;
							else
								;
						else if (name.equals(TerminalData.PASSWORD))
							if (terminalData2.getChild(1) instanceof TerminalData.Authentication.Password)
								if (terminalData2.getChild(1).close())
									return;
								else
									;
							else
								;
						else
							;
					else if (terminalData2 instanceof TerminalData.Message)
						if (name.equals(TerminalData.SUBSCRIPTIONID))
							if (terminalData2.getChild(0) instanceof TerminalData.Message.SubscriptionID)
								if (terminalData2.getChild(0).close())
									return;
								else
									;
							else
								;
						else if (name.equals(TerminalData.MESSAGEBODY))
							if (terminalData2.getChild(1) instanceof TerminalData.Message.MessageBody)
								if (terminalData2.getChild(1).close())
									return;
								else
									;
							else
								;
						else
							;
					else
						;

				}

			}

			System.err.println("Illegal End (IGNORING) : " + name);

		}

	}

	@SuppressWarnings("unused")
	private static void printEvent(XMLStreamReader xmlr) {

		System.out.print("EVENT:[" + xmlr.getLocation().getLineNumber() + "]["
				+ xmlr.getLocation().getColumnNumber() + "] ");

		System.out.print(" [");

		switch (xmlr.getEventType()) {

		case XMLStreamConstants.START_ELEMENT:
			System.out.print("<");
			printName(xmlr);
			printNamespaces(xmlr);
			printAttributes(xmlr);
			System.out.print(">");
			break;

		case XMLStreamConstants.END_ELEMENT:
			System.out.print("</");
			printName(xmlr);
			System.out.print(">");
			break;

		case XMLStreamConstants.SPACE:

		case XMLStreamConstants.CHARACTERS:
			int start = xmlr.getTextStart();
			int length = xmlr.getTextLength();
			System.out
					.print(new String(xmlr.getTextCharacters(), start, length));
			break;

		case XMLStreamConstants.PROCESSING_INSTRUCTION:
			System.out.print("<?");
			if (xmlr.hasText())
				System.out.print(xmlr.getText());
			System.out.print("?>");
			break;

		case XMLStreamConstants.CDATA:
			System.out.print("<![CDATA[");
			start = xmlr.getTextStart();
			length = xmlr.getTextLength();
			System.out
					.print(new String(xmlr.getTextCharacters(), start, length));
			System.out.print("]]>");
			break;

		case XMLStreamConstants.COMMENT:
			System.out.print("<!--");
			if (xmlr.hasText())
				System.out.print(xmlr.getText());
			System.out.print("-->");
			break;

		case XMLStreamConstants.ENTITY_REFERENCE:
			System.out.print(xmlr.getLocalName() + "=");
			if (xmlr.hasText())
				System.out.print("[" + xmlr.getText() + "]");
			break;

		case XMLStreamConstants.START_DOCUMENT:
			System.out.print("<?xml");
			System.out.print(" version='" + xmlr.getVersion() + "'");
			System.out.print(" encoding='" + xmlr.getCharacterEncodingScheme()
					+ "'");
			if (xmlr.isStandalone())
				System.out.print(" standalone='yes'");
			else
				System.out.print(" standalone='no'");
			System.out.print("?>");
			break;

		case XMLStreamConstants.END_DOCUMENT:
			System.out.println("<DOCUMENT END>");
			break;

		}
		System.out.println("]");
	}

	private static void printName(XMLStreamReader xmlr) {
		if (xmlr.hasName()) {
			String prefix = xmlr.getPrefix();
			String uri = xmlr.getNamespaceURI();
			String localName = xmlr.getLocalName();
			printName(prefix, uri, localName);
		}
	}

	private static void printName(String prefix, String uri, String localName) {
		if (uri != null && !("".equals(uri)))
			System.out.print("['" + uri + "']:");
		if (prefix != null)
			System.out.print(prefix + ":");
		if (localName != null)
			System.out.print(localName);
	}

	private static void printAttributes(XMLStreamReader xmlr) {
		for (int i = 0; i < xmlr.getAttributeCount(); i++) {
			printAttribute(xmlr, i);
		}
	}

	private static void printAttribute(XMLStreamReader xmlr, int index) {
		String prefix = xmlr.getAttributePrefix(index);
		String namespace = xmlr.getAttributeNamespace(index);
		String localName = xmlr.getAttributeLocalName(index);
		String value = xmlr.getAttributeValue(index);
		System.out.print(" ");
		printName(prefix, namespace, localName);
		System.out.print("='" + value + "'");
	}

	private static void printNamespaces(XMLStreamReader xmlr) {
		for (int i = 0; i < xmlr.getNamespaceCount(); i++) {
			printNamespace(xmlr, i);
		}
	}

	private static void printNamespace(XMLStreamReader xmlr, int index) {
		String prefix = xmlr.getNamespacePrefix(index);
		String uri = xmlr.getNamespaceURI(index);
		System.out.print(" ");
		if (prefix == null)
			System.out.print("xmlns='" + uri + "'");
		else
			System.out.print("xmlns:" + prefix + "='" + uri + "'");
	}
	
}
