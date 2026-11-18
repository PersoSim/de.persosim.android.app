package de.persosim.driver.connector.pcsc;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.persosim.driver.connector.IfdInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.simulator.utils.HexString;

/**
 * This class contains all data that is transmitted when a PCSC function call
 * occurs.
 * 
 * @author mboonk
 * 
 */
public class PcscCallData {
	private UnsignedInteger function;
	private UnsignedInteger logicalUnitNumber;
	private List<byte[]> parameters;

	/**
	 * Create a new instance by parsing the given data.
	 * @param data
	 */
	public PcscCallData(String data) {
		function = getCallType(data);
		logicalUnitNumber = getLogicalUnitNumber(data);
		parameters = getParameters(data);
	}
	
	public PcscCallData(UnsignedInteger function, UnsignedInteger logicalUnitNumber, List<byte[]> parameters) {
		this.function = function;
		this.logicalUnitNumber = logicalUnitNumber;
		this.parameters = parameters;
	}

	public void setFunction(UnsignedInteger function) {
		this.function = function;
	}

	/**
	 * @return the function
	 */
	public UnsignedInteger getFunction() {
		return function;
	}

	/**
	 * @return the logicalUnitNumber
	 */
	public UnsignedInteger getLogicalUnitNumber() {
		return logicalUnitNumber;
	}

	/**
	 * @return the parameters
	 */
	public List<byte[]> getParameters() {
		return parameters;
	}

	public void setParameters(List<byte[]> parameters) {
		this.parameters = parameters;
	}

	private List<byte[]> getParameters(String data) {
		List<byte []> result = new ArrayList<byte []>();
		String [] dataArray = data.split(Pattern.quote(IfdInterface.MESSAGE_DIVIDER));
		for (int i = 2; i < dataArray.length; i++){
			result.add(HexString.toByteArray(dataArray[i]));
		}
		return result;
	}

	private UnsignedInteger getLogicalUnitNumber(String data) {
		String [] dataArray = data.split(Pattern.quote(IfdInterface.MESSAGE_DIVIDER));
		if (dataArray.length < 1){
			throw new InvalidParameterException("Given data does not contain a logical number");
		}
		return UnsignedInteger.parseUnsignedInteger(dataArray[1], 16);
	}

	private UnsignedInteger getCallType(String data) {
		String [] dataArray = data.split(Pattern.quote(IfdInterface.MESSAGE_DIVIDER));
		if (dataArray.length < 2){
			throw new InvalidParameterException("Given data does not contain a function number");
		}
		return UnsignedInteger.parseUnsignedInteger(dataArray[0], 16);
	}
	
	@Override
	public String toString() {
		String result = function.getAsHexString() + IfdInterface.MESSAGE_DIVIDER + logicalUnitNumber.getAsHexString();
		for (byte [] current : parameters){
			result += IfdInterface.MESSAGE_DIVIDER + HexString.encode(current);
		}
		return result;
	}
}
