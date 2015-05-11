package de.persosim.android.app;

import java.io.File;

import android.os.Environment;



public interface Constants {
	
	public static final String lineSeparator = System.getProperty("line.separator");
	
	public static final File   DIR_DOWNLOAD                    = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	
	public static final String DIR_DOWNLOAD_NAME               = DIR_DOWNLOAD.toString();
	public static final String DIR_FELIX_NAME                  = DIR_DOWNLOAD    + "/felixbase";
	public static final String DIR_BUNDLE_NAME                 = DIR_FELIX_NAME  + "/bundles";
	public static final String DIR_BUNDLE_ORDERED_START_NAME   = DIR_BUNDLE_NAME + "/ordered";
	public static final String DIR_BUNDLE_UNORDERED_START_NAME = DIR_BUNDLE_NAME + "/unordered";
//	public static final String DIR_BUNDLE_NO_START_NAME        = DIR_BUNDLE_NAME + "/nostart";
	public static final String DIR_PERSO_NAME                  = DIR_FELIX_NAME  + "/personalization";
	
	public static final String BOOT_DELEGATION_PACKAGES = (
			"org.osgi.*," +
			"android.*," +
			"com.google.android.*," +
			"javax.*," +
			"org.apache.commons.*," +
	        "org.bluez," + 
	        "org.json," + 
	        "org.w3c.dom," + 
	        "org.xml.*"
			).intern();
	
	public static final String ANDROID_FRAMEWORK_PACKAGES = (
			"com.example.foo"
	        ).intern();
		
	public static final String ANDROID_FRAMEWORK_PACKAGES_ext = (
			/*
			 * org.osgi
			 */
			"org.osgi.framework; version=1.4.0," +
	        "org.osgi.service.packageadmin; version=1.2.0," +
	        "org.osgi.service.startlevel; version=1.0.0," +
	        "org.osgi.service.url; version=1.0.0," +
	        "org.osgi.util.tracker," +
	        /*
	         * android
	         * Use semicolon as separator starting from here
	         */
	        "android; " + 
	        "android.app;" + 
	        "android.content;" + 
	        "android.database;" + 
	        "android.database.sqlite;" + 
	        "android.graphics; " + 
	        "android.graphics.drawable; " + 
	        "android.graphics.glutils; " + 
	        "android.hardware; " + 
	        "android.location; " + 
	        "android.media; " + 
	        "android.net; " + 
	        "android.opengl; " + 
	        "android.os; " + 
	        "android.provider; " + 
	        "android.sax; " + 
	        "android.speech.recognition; " + 
	        "android.telephony; " + 
	        "android.telephony.gsm; " + 
	        "android.text; " + 
	        "android.text.method; " + 
	        "android.text.style; " + 
	        "android.text.util; " + 
	        "android.util; " + 
	        "android.view; " + 
	        "android.view.animation; " + 
	        "android.webkit; " + 
	        "android.widget; " + 
	        /*
	         * com.google
	         */
	        "com.google.android.maps; " + 
	        "com.google.android.xmppService; " + 
	        /*
	         * javax
	         */
	        "javax.crypto; " + 
	        "javax.crypto.interfaces; " + 
	        "javax.crypto.spec; " + 
	        "javax.microedition.khronos.opengles; " + 
	        "javax.net; " + 
	        "javax.net.ssl; " + 
	        "javax.security.auth; " + 
	        "javax.security.auth.callback; " + 
	        "javax.security.auth.login; " + 
	        "javax.security.auth.x500; " + 
	        "javax.security.cert; " + 
	        "javax.sound.midi; " + 
	        "javax.sound.midi.spi; " + 
	        "javax.sound.sampled; " + 
	        "javax.sound.sampled.spi; " + 
	        "javax.sql; " + 
	        "javax.xml.parsers; " + 
	        /*
	         * junit
	         */
	        "junit.extensions; " + 
	        "junit.framework; " + 
	        /*
	         * org.apache.commons
	         */
	        "org.apache.commons.codec; " + 
	        "org.apache.commons.codec.binary; " + 
	        "org.apache.commons.codec.language; " + 
	        "org.apache.commons.codec.net; " + 
	        "org.apache.commons.httpclient; " + 
	        "org.apache.commons.httpclient.auth; " + 
	        "org.apache.commons.httpclient.cookie; " + 
	        "org.apache.commons.httpclient.methods; " + 
	        "org.apache.commons.httpclient.methods.multipart; " + 
	        "org.apache.commons.httpclient.params; " + 
	        "org.apache.commons.httpclient.protocol; " + 
	        "org.apache.commons.httpclient.util; " + 
	        /*
	         * miscellaneous
	         */
	        "org.bluez; " + 
	        "org.json; " + 
	        "org.w3c.dom; " + 
	        "org.xml.sax; " + 
	        "org.xml.sax.ext; " + 
	        "org.xml.sax.helpers; " + 
	        /*
	         * Android OS Version?
	         * Stop using semicolon as separator starting from here
	         */
	        "version=1.5.0.r3," +
	        /*
	         * define own packages below
	         */
			"com.example.foo," +
			"com.example.bar"
	        
			).intern();
	
}
