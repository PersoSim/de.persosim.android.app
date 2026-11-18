package de.persosim.driver.connector;

import java.io.IOException;

/**
 * This interface describes the user interfaces of the virtual reader, e.g. a
 * pin pad and a display.
 * 
 * @author mboonk
 *
 */
public interface VirtualReaderUi {
	/**
	 * Prompt the user to enter a PIN
	 * 
	 * @return the PIN data as byte array or null if the user canceled the
	 *         process
	 * @throws IOException
	 */
	public abstract byte[] getPin() throws IOException;

	/**
	 * Show a string based display to the user.
	 * 
	 * @param lines
	 *            the lines to display a multiple {@link String} objects
	 */
	public abstract void display(String... lines);

	/**
	 * @return the PCSC device descriptors for this user interface
	 */
	public abstract byte[] getDeviceDescriptors();

	/**
	 * Update the ICC presence indicator (if applicable)
	 * @param iccPresent
	 */
	public abstract void iccPresence(boolean iccPresent);

	/**
	 * Display the chat to be used for the following authentication
	 * @param chat
	 */
	public abstract void displayChat(byte[] chat);
}
