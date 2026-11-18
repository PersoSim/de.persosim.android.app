package de.persosim.driver.connector.pcsc;

/**
 * This interface defines a PCSC feature.
 * 
 * @author mboonk
 * 
 */
public interface PcscFeature extends PcscListener {

	/**
	 * This is a concatenation of the one byte feature description, a length
	 * byte (static value of 4) and a control code to be used by the pcsc system
	 * in deviceControl executions.<br/>
	 * 
	 * These features are defined in the PCSC specification part 10 2.2
	 * @return the feature definition
	 */
	public abstract byte[] getFeatureDefinition();
}
