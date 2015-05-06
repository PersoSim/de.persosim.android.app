package de.persosim.android.app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;



public class Utils {
	
	public static final String HEXCHARACTERS = "0123456789ABCDEF";
	
	public static final String LOG_TAG = Utils.class.getName();
	
	

	/**
     * Utility method to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     * @throws java.lang.IllegalArgumentException if input length is incorrect
     */
	// TODO copied from PersoSim
    public static byte[] HexStringToByteArray(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }
        byte[] data = new byte[len / 2]; // Allocate 1 byte per 2 hex characters
        for (int i = 0; i < len; i += 2) {
            // Convert each character into a integer (base-16), then bit-shift into place
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    /**
	 * This method returns a String representation of a byte array
	 * @param input the byte array to be represented as String
	 * @return a String representation of the provided byte array
	 */
    // TODO copied from PersoSim
	public static String encode(byte[] input) {
		if (input == null) {
	      return "";
	    }
		
		StringBuilder builder = new StringBuilder(2*input.length);
		
	    for (byte b : input) {
		  builder.append(HEXCHARACTERS.charAt((b & 0xF0) >> 4)).append(HEXCHARACTERS.charAt((b & 0x0F)));
		}
	    
		return builder.toString();
	}
	
	/**
	 * Returns an unsigned byte array representation of an unsigned short
	 * Returned Array has length 2; unused bytes are padded to 0x00.
	 * @param input the short
	 * @return the unsigned byte array representation of the unsigned short
	 */
	// TODO copied from PersoSim
	public static byte[] toUnsignedByteArray(short input) {
		return new byte[]{(byte) ((input & (short) 0xFF00) >> 8), (byte) (input & (short) 0x00FF)};
	}
	
	/**
	 * Returns a concatenation of one or more byte arrays
	 * @param byteArrays one or more byte arrays
	 * @return a concatenation of one or more byte arrays
	 */
	// TODO copied from PersoSim
	public static byte[] concatByteArrays(byte[]... byteArrays) {
		if ( byteArrays == null || byteArrays.length == 0 ) {throw new IllegalArgumentException( "parameters must not be null or empty" );}
		
		ByteArrayOutputStream outputStream;
		
		outputStream = new ByteArrayOutputStream();
		
		for(byte[] currentByteArray : byteArrays) {
			try {
				outputStream.write(currentByteArray);
			} catch (IOException e) {
				//do nothing
			}
		}
		
		return outputStream.toByteArray();
	}
	
	/**
	 * This method returns a String representation of the bundle state provided
	 * as int according to the mappings of the variables provided in
	 * org.osgi.framework.Bundle.
	 * 
	 * @param state the bundle state
	 * @return a String representation of the bundle state
	 */
	public static String translateState(int state) {
		switch (state) {
        case org.osgi.framework.Bundle.UNINSTALLED: return "UNINSTALLED";
        case org.osgi.framework.Bundle.INSTALLED: return "INSTALLED";
        case org.osgi.framework.Bundle.RESOLVED: return "RESOLVED";
        case org.osgi.framework.Bundle.STARTING: return "STARTING";
        case org.osgi.framework.Bundle.ACTIVE: return "ACTIVE";
        case org.osgi.framework.Bundle.STOPPING: return "STOPPING";
        default: return "UNKNOWN";
		}
	}
	
	/**
	 * This method returns a list containing all files matching the provided file extension to be found
	 * recursively at the exact directory of the provided File object or in sub folders.
	 * 
	 * @param startFolder the folder to start at
	 * @param fileExtension the file extension for which is to be searched
	 * @param recursive whether the folder is to be searched recursively including sub folders
	 * @return a list of all contained *.jar files
	 */
	public static List<File> listJarFilesForFolder(File startFolder, String fileExtension, boolean recursive) {
		ArrayList<File> allBundleFiles = new ArrayList<>();
		
		if ((startFolder == null) || (!startFolder.exists()) || (!startFolder.isDirectory())) {
            return allBundleFiles;
        }
		
		for (final File currentFile : startFolder.listFiles()) {
	        if (currentFile.isDirectory() && recursive) {
	            allBundleFiles.addAll(listJarFilesForFolder(currentFile, fileExtension, recursive));
	        } else {
	            if(currentFile.getName().toLowerCase(Locale.getDefault()).endsWith(fileExtension)) {
	            	allBundleFiles.add(currentFile);
	            }
	        }
	    }
		
		return allBundleFiles;
	}
	
	public static List<String> listFileNamesForFolder(String startFolderName, String fileExtension, boolean recursive) {
		ArrayList<String> allBundleFileNames = new ArrayList<>();
		
		if(startFolderName == null) {
			return allBundleFileNames;
		}
		
		File folder = new File(startFolderName);
		List<File> allBundleFiles = listJarFilesForFolder(folder, fileExtension, recursive);
		
		for(File currentFile:allBundleFiles) {
			allBundleFileNames.add(currentFile.getAbsolutePath());
		}
		
		return allBundleFileNames;
	}
	
	/**
	 * This method exports Android raw resources as files to disk.
	 * @param context the content to request the resources from
	 * @param resource the resource identifier
	 * @param fileName the file name incl. path where the resource is to be exported to.
	 */
	public static void writeRawResourceToFile(Context context, int resource, String pathName, String fileName) {
		Log.d(LOG_TAG, "START writeRawResourceToFile(int, String)");
		
		OutputStream outputStream = null;

		Resources res = context.getResources();
		InputStream inputStream = res.openRawResource(resource);
		
		File file = new File(pathName, fileName);
		File path = new File(pathName);
		path.mkdirs();
		
		try {
			
			outputStream = new FileOutputStream(file);

			int bytesLeftToRead = 0;
			byte[] bytes = new byte[1024];

			while ((bytesLeftToRead = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, bytesLeftToRead);
			}

			Log.d(LOG_TAG, "resource successfully written to file " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					Log.d(LOG_TAG, "failed to write resource to file " + file.getAbsolutePath());
					e.printStackTrace();
				}
			}
			
			if (outputStream != null) {
				try {
					outputStream.flush();
					outputStream.close();

				} catch (IOException e) {
					Log.d(LOG_TAG, "failed to write resource to file " + fileName);
					e.printStackTrace();
				}

			}
		}
		
		Log.d(LOG_TAG, "END writeRawResourceToFile(int, String)");
	}
	
	/**
	 * This method creates the path to the provided file if not existing.
	 * @param file the file indicating the path to check for
	 */
	public static void preparePath(File file) {
		if (file == null) {
			Log.w(LOG_TAG, "file reference is null");
		} else {
			if (file.exists()) {
				Log.d(LOG_TAG, "file " + file.getName() + " already exists");
			} else {
				if (file.mkdirs()) {
					Log.d(LOG_TAG, "created missing directory: " + file.getAbsolutePath());
				} else {
					Log.w(LOG_TAG, "unable to create missing directory: " + file.getAbsolutePath());
				}
			}
		}
	}
	
}
