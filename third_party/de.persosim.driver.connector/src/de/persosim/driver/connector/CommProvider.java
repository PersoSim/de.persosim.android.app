package de.persosim.driver.connector;

public interface CommProvider {

	/**
	 * Build a working {@link IfdComm} instance if this provider can create comm
	 * objects for the given type.
	 * 
	 * @param type
	 *            or null, if null is given, the provider will unconditionally try
	 *            to construct an {@link IfdComm} object
	 * @return an {@link IfdComm} instance or null
	 */
	public IfdComm get(String type);

}
