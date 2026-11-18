package de.persosim.websocket;

public class Tr03112codes {
	private static final String RESULT_MAJOR = "http://www.bsi.bund.de/ecard/api/1.1/resultmajor#";
	public static final String RESULT_MAJOR_OK = RESULT_MAJOR + "ok";
	public static final String RESULT_MAJOR_ERROR = RESULT_MAJOR + "error";
	public static final String RESULT_MAJOR_WARNING = RESULT_MAJOR + "warning";
	private static final String RESULT_MINOR = "http://www.bsi.bund.de/ecard/api/1.1/resultminor/";
	private static final String IFDL = "ifdl";
	private static final String TERMINAL_RESULT = RESULT_MINOR + IFDL + "/";
	
	private static final String TERMINAL_RESULT_COMMON = TERMINAL_RESULT + "common#";
	
	public static final String TERMINAL_RESULT_COMMON_TIMEOUT_ERROR = TERMINAL_RESULT_COMMON + "timeoutError";
	public static final String TERMINAL_RESULT_COMMON_TIMEOUT_INVALID_CONTEXT_HANDLE = TERMINAL_RESULT_COMMON + "invalidContextHandle";
	public static final String TERMINAL_RESULT_COMMON_TIMEOUT_CANCELLATION_BY_USER = TERMINAL_RESULT_COMMON + "cancellationByUser";
	public static final String TERMINAL_RESULT_COMMON_TIMEOUT_UNKNOWN_SESSION_IDENTIFIER = TERMINAL_RESULT_COMMON + "unknownSessionIdentifier";
	public static final String TERMINAL_RESULT_COMMON_TIMEOUT_INVALID_SLOT_HANDLE = TERMINAL_RESULT_COMMON + "invalidSlotHandle";
	public static final String TERMINAL_RESULT_COMMON_UNSUPPORTED_PROTOCOL = TERMINAL_RESULT_COMMON + "unsupportedProtocol";
	
	private static final String TERMINAL_RESULT_TERMINAL = "terminal#";

	public static final String TERMINAL_RESULT_TERMINAL_UNKNOWN_IFD = TERMINAL_RESULT_TERMINAL + "unknownIFD";
	public static final String TERMINAL_RESULT_TERMINAL_NO_CARD = TERMINAL_RESULT_TERMINAL + "noCard";
	public static final String TERMINAL_RESULT_TERMINAL_IFD_SHARING_VIOLATION = TERMINAL_RESULT_TERMINAL + "IFDSharingViolation";
	public static final String TERMINAL_RESULT_TERMINAL_UNKNOWN_ACTION = TERMINAL_RESULT_TERMINAL + "unknownAction";
	public static final String TERMINAL_RESULT_TERMINAL_UNKNOWN_SLOT = TERMINAL_RESULT_TERMINAL + "unknownSlot";
	public static final String TERMINAL_RESULT_TERMINAL_ACCESS_ERROR = TERMINAL_RESULT_TERMINAL + "accessError";
	
	
}
