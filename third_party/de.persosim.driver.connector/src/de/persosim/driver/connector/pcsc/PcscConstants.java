package de.persosim.driver.connector.pcsc;

import de.persosim.driver.connector.UnsignedInteger;


/**
 * This interface defines often used values as specified in the PCSC specification.
 * @author mboonk
 *
 */
public interface PcscConstants {
	public static final int VALUE_IFD_POWER_UP = 500;
	public static final int VALUE_IFD_POWER_DOWN = 501;
	public static final int VALUE_IFD_RESET = 502;
	public static final byte IFD_NEGOTIATE_PTS1 = 1;
	public static final byte IFD_NEGOTIATE_PTS2 = 2;
	public static final byte IFD_NEGOTIATE_PTS3 = 4;
	public static final int VALUE_IFD_SUCCESS = 0;
	public static final int VALUE_IFD_ERROR_TAG = 600;
	public static final int VALUE_IFD_ERROR_SET_FAILURE = 601;
	public static final int VALUE_IFD_ERROR_VALUE_READ_ONLY = 602;
	public static final int VALUE_IFD_ERROR_PTS_FAILURE = 605;
	public static final int VALUE_IFD_ERROR_NOT_SUPPORTED = 606;
	public static final int VALUE_IFD_PROTOCOL_NOT_SUPPORTED = 607;
	public static final int VALUE_IFD_ERROR_POWER_ACTION = 608;
	public static final int VALUE_IFD_ERROR_SWALLOW = 609;
	public static final int VALUE_IFD_ERROR_EJECT = 610;
	public static final int VALUE_IFD_ERROR_CONFISCATE = 611;
	public static final int VALUE_IFD_COMMUNICATION_ERROR = 612;
	public static final int VALUE_IFD_RESPONSE_TIMEOUT = 613;
	public static final int VALUE_IFD_NOT_SUPPORTED = 614;
	public static final int VALUE_IFD_ICC_PRESENT = 615;
	public static final int VALUE_IFD_ICC_NOT_PRESENT = 616;
	public static final int VALUE_IFD_NO_SUCH_DEVICE = 617;
	public static final int VALUE_IFD_ERROR_INSUFFICIENT_BUFFER = 618;

	public static final int VALUE_TAG_VENDOR_NAME = 0x0100;
	public static final int VALUE_TAG_VENDOR_TYPE = 0x0101;
	public static final int VALUE_TAG_VENDOR_VERSION = 0x0102;
	public static final int VALUE_TAG_VENDOR_SERIAL = 0x0103;

	public static final int VALUE_TAG_IFD_ATR = 0x0303;
	public static final int VALUE_TAG_IFD_SLOTNUM = 0x0180;
	public static final int VALUE_TAG_IFD_SLOT_THREAD_SAFE = 0x0FAC;
	public static final int VALUE_TAG_IFD_THREAD_SAFE = 0x0FAD;
	public static final int VALUE_TAG_IFD_SLOTS_NUMBER = 0x0FAE;
	public static final int VALUE_TAG_IFD_SIMULTANEOUS_ACCESS = 0x0FAF;
	public static final int VALUE_TAG_IFD_POLLING_THREAD = 0x0FB0;
	public static final int VALUE_TAG_IFD_POLLING_THREAD_KILLABLE = 0x0FB1;
	public static final int VALUE_SCARD_PROTOCOL_T0 = 1;
	public static final int VALUE_SCARD_PROTOCOL_T1 = 2;
	public static final int VALUE_SCARD_PROTOCOL_ANY = VALUE_SCARD_PROTOCOL_T0 | VALUE_SCARD_PROTOCOL_T1;
	public static final int VALUE_SCARD_PROTOCOL_RAW = 4;
	public static final int VALUE_SCARD_PROTOCOL_T15 = 8;

	public static final int VALUE_DEVICE_TYPE_SLOT = 1;
	public static final int VALUE_DEVICE_TYPE_FUNCTIONAL = 1;
	/**
	 * Standard control code to be supported by all class 2 readers as defined
	 * in the PCSC specification.
	 */
	public static final int VALUE_CONTROL_CODE_GET_FEATURE_REQUEST = 0x42000d48;
	

	public static final UnsignedInteger IFD_POWER_UP = new UnsignedInteger(VALUE_IFD_POWER_UP);
	public static final UnsignedInteger IFD_POWER_DOWN = new UnsignedInteger(VALUE_IFD_POWER_DOWN);
	public static final UnsignedInteger IFD_RESET = new UnsignedInteger(VALUE_IFD_RESET);
	public static final UnsignedInteger IFD_SUCCESS = new UnsignedInteger(VALUE_IFD_SUCCESS);
	public static final UnsignedInteger IFD_ERROR_TAG = new UnsignedInteger(VALUE_IFD_ERROR_TAG);
	public static final UnsignedInteger IFD_ERROR_SET_FAILURE = new UnsignedInteger(VALUE_IFD_ERROR_SET_FAILURE);
	public static final UnsignedInteger IFD_ERROR_VALUE_READ_ONLY = new UnsignedInteger(VALUE_IFD_ERROR_VALUE_READ_ONLY);
	public static final UnsignedInteger IFD_ERROR_PTS_FAILURE = new UnsignedInteger(VALUE_IFD_ERROR_PTS_FAILURE);
	public static final UnsignedInteger IFD_ERROR_NOT_SUPPORTED = new UnsignedInteger(VALUE_IFD_ERROR_NOT_SUPPORTED);
	public static final UnsignedInteger IFD_PROTOCOL_NOT_SUPPORTED = new UnsignedInteger(VALUE_IFD_PROTOCOL_NOT_SUPPORTED);
	public static final UnsignedInteger IFD_ERROR_POWER_ACTION = new UnsignedInteger(VALUE_IFD_ERROR_POWER_ACTION);
	public static final UnsignedInteger IFD_ERROR_SWALLOW = new UnsignedInteger(VALUE_IFD_ERROR_SWALLOW);
	public static final UnsignedInteger IFD_ERROR_EJECT = new UnsignedInteger(VALUE_IFD_ERROR_EJECT);
	public static final UnsignedInteger IFD_ERROR_CONFISCATE = new UnsignedInteger(VALUE_IFD_ERROR_CONFISCATE);
	public static final UnsignedInteger IFD_COMMUNICATION_ERROR = new UnsignedInteger(VALUE_IFD_COMMUNICATION_ERROR);
	public static final UnsignedInteger IFD_RESPONSE_TIMEOUT = new UnsignedInteger(VALUE_IFD_RESPONSE_TIMEOUT);
	public static final UnsignedInteger IFD_NOT_SUPPORTED = new UnsignedInteger(VALUE_IFD_NOT_SUPPORTED);
	public static final UnsignedInteger IFD_ICC_PRESENT = new UnsignedInteger(VALUE_IFD_ICC_PRESENT);
	public static final UnsignedInteger IFD_ICC_NOT_PRESENT = new UnsignedInteger(VALUE_IFD_ICC_NOT_PRESENT);
	public static final UnsignedInteger IFD_NO_SUCH_DEVICE = new UnsignedInteger(VALUE_IFD_NO_SUCH_DEVICE);
	public static final UnsignedInteger IFD_ERROR_INSUFFICIENT_BUFFER = new UnsignedInteger(VALUE_IFD_ERROR_INSUFFICIENT_BUFFER);
	public static final UnsignedInteger TAG_VENDOR_NAME = new UnsignedInteger(VALUE_TAG_VENDOR_NAME);
	public static final UnsignedInteger TAG_VENDOR_TYPE = new UnsignedInteger(VALUE_TAG_VENDOR_TYPE);
	public static final UnsignedInteger TAG_VENDOR_VERSION = new UnsignedInteger(VALUE_TAG_VENDOR_VERSION);
	public static final UnsignedInteger TAG_VENDOR_SERIAL = new UnsignedInteger(VALUE_TAG_VENDOR_SERIAL);
	public static final UnsignedInteger TAG_IFD_ATR = new UnsignedInteger(VALUE_TAG_IFD_ATR);
	public static final UnsignedInteger TAG_IFD_SLOTNUM = new UnsignedInteger(VALUE_TAG_IFD_SLOTNUM);
	public static final UnsignedInteger TAG_IFD_SLOT_THREAD_SAFE = new UnsignedInteger(VALUE_TAG_IFD_SLOT_THREAD_SAFE);
	public static final UnsignedInteger TAG_IFD_THREAD_SAFE = new UnsignedInteger(VALUE_TAG_IFD_THREAD_SAFE);
	public static final UnsignedInteger TAG_IFD_SLOTS_NUMBER = new UnsignedInteger(VALUE_TAG_IFD_SLOTS_NUMBER);
	public static final UnsignedInteger TAG_IFD_SIMULTANEOUS_ACCESS = new UnsignedInteger(VALUE_TAG_IFD_SIMULTANEOUS_ACCESS);
	public static final UnsignedInteger TAG_IFD_POLLING_THREAD = new UnsignedInteger(VALUE_TAG_IFD_POLLING_THREAD);
	public static final UnsignedInteger TAG_IFD_POLLING_THREAD_KILLABLE = new UnsignedInteger(VALUE_TAG_IFD_POLLING_THREAD_KILLABLE);
	public static final UnsignedInteger SCARD_PROTOCOL_T0 = new UnsignedInteger(VALUE_SCARD_PROTOCOL_T0);
	public static final UnsignedInteger SCARD_PROTOCOL_T1 = new UnsignedInteger(VALUE_SCARD_PROTOCOL_T1);
	public static final UnsignedInteger SCARD_PROTOCOL_ANY = new UnsignedInteger(VALUE_SCARD_PROTOCOL_ANY);
	public static final UnsignedInteger SCARD_PROTOCOL_RAW = new UnsignedInteger(VALUE_SCARD_PROTOCOL_RAW);
	public static final UnsignedInteger SCARD_PROTOCOL_T15 = new UnsignedInteger(VALUE_SCARD_PROTOCOL_T15);
	public static final UnsignedInteger DEVICE_TYPE_SLOT = new UnsignedInteger(VALUE_DEVICE_TYPE_SLOT);
	public static final UnsignedInteger DEVICE_TYPE_FUNCTIONAL = new UnsignedInteger(VALUE_DEVICE_TYPE_FUNCTIONAL);
	public static final UnsignedInteger CONTROL_CODE_GET_FEATURE_REQUEST = new UnsignedInteger(VALUE_CONTROL_CODE_GET_FEATURE_REQUEST);

}
