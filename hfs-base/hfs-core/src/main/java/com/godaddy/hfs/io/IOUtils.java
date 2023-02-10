package com.godaddy.hfs.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);
	
	public static void closeQuietly(AutoCloseable closeable) {
		try {
			closeable.close();
		} catch (Exception e) {
			logger.warn("Error closing resource", e);
		}
	}
	
	/**
	 * Close the given AutoCloseable instances in the order provided
	 *   (left to right parameter order)
	 * @param closeables
	 */
	public static void closeQuietly(AutoCloseable...closeables) {
		for (AutoCloseable closeable : closeables) {
			closeQuietly(closeable);
		}
	}
	
	public static void closeQuietly(Iterable<? extends AutoCloseable> closeables) {
	    for (AutoCloseable closeable : closeables) {
	        closeQuietly(closeable);
	    }
	}
	
	public static String readFully(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		char[] buf = new char[4096];
		int charsRead = -1;
		while ((charsRead = reader.read(buf)) != -1) {
			sb.append(buf, 0, charsRead);
		}
		
		return sb.toString();
	}
	
	protected IOUtils() {
		// defeat instantiation
	}

}
