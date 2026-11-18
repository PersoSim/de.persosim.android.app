package de.persosim.driver.connector.pcsc;

import java.util.Collection;
import java.util.List;

import de.persosim.driver.connector.VirtualReaderUi;

/**
 * This interface describes the ability to use a {@link VirtualReaderUi} to get
 * or present data.
 * 
 * @author mboonk
 *
 */
public interface UiEnabled {
	/**
	 * @param interfaces
	 *            set a new {@link Collection} of user interfaces to use
	 */
	public abstract void setUserInterfaces(
			List<VirtualReaderUi> interfaces);
}
