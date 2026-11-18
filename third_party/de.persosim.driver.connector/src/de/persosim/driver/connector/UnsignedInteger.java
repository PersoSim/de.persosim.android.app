package de.persosim.driver.connector;

import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

/**
 * This class represents an unsigned integer value with a length of 32 bit.
 * @author mboonk
 *
 */
public class UnsignedInteger {
	private long value;
	private static final long MAX_VALUE_LONG = 0x0FFFFFFFFl;
	private static final long MIN_VALUE_LONG = 0;
	public static final UnsignedInteger MAX_VALUE = new UnsignedInteger(MAX_VALUE_LONG);
	public static final UnsignedInteger MIN_VALUE = new UnsignedInteger(MIN_VALUE_LONG);
	
	public UnsignedInteger(long value){
		if (value > MAX_VALUE_LONG || value < 0 ){
			throw new NumberFormatException("Value to big for this type");
		}
		this.value = value;
	}
	
	/**
	 * Constructs an {@link UnsignedInteger} from an integer value. The integer
	 * is interpreted as if it contains the bits of an unsigned value, the sign
	 * bit is used as the MSB.
	 * 
	 * @param value
	 */
	public UnsignedInteger(int value){
		this.value = value & 0xFFFFFFFFl;
	}
	
	/**
	 * Constructs an {@link UnsignedInteger} from a byte array.
	 * @see Utils#getIntFromUnsignedByteArray(byte[])
	 * @param data
	 */
	public UnsignedInteger(byte [] data){
		if (data.length > 4){
			throw new NumberFormatException("Source byte array to long");
		}
		this.value = Utils.getIntFromUnsignedByteArray(data) & 0xFFFFFFFFl;
	}
	
	/**
	 * @return all bits of the unsigned integer value, using the sign bit to
	 *         store the MSB
	 */
	public int getAsInt(){
		return (int) value;
	}
	
	/**
	 * Parse the given string as a unsigned integer value.
	 * @see Integer#parseInt(String, int)
	 * @param value
	 * @param radix
	 * @return
	 */
	public static UnsignedInteger parseUnsignedInteger(String value, int radix){
		long newValue = Long.parseLong(value, radix);
		return new UnsignedInteger(newValue);
	}
	
	/**
	 * @return the value encoded in a 4 byte (8 nibbles) long hex string
	 */
	public String getAsHexString(){
		return HexString.encode(getAsByteArray());
	}
	
	/**
	 * @see Utils#toUnsignedByteArray(int)
	 * @return the value stored in a 4 byte long array
	 */
	public byte [] getAsByteArray(){
		return Utils.toUnsignedByteArray(getAsInt());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnsignedInteger other = (UnsignedInteger) obj;
		if (value != other.value)
			return false;
		return true;
	}

	/**
	 * @return the value stored in a signed 64 bit long number
	 */
	public long getAsSignedLong() {
		return value;
	}
}
