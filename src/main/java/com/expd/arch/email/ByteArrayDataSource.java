package com.expd.arch.email;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.activation.DataSource;




/**
 * ByteArrayDataSource implements a DataSource from:
 * 	an InputStream
 *	a byte array
 * 	a String
 */
public class ByteArrayDataSource implements DataSource {
	private byte[] data; // data
	private String type; // content-type

	public ByteArrayDataSource(byte[] data, String type) {
		this.data = data;
		this.type = type;
	}
	
	public ByteArrayDataSource(InputStream is, String type) {
		this.type = type;
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int ch;

			while ((ch = is.read()) != -1)
				// XXX - must be made more efficient by
				// doing buffered reads, rather than one byte reads
				os.write(ch);
			data = os.toByteArray();

		} catch (IOException ioex) {
		}
	}
	
	public ByteArrayDataSource(String data, String type) {
		try {
			// Assumption that the string contains only ASCII
			// characters!  Otherwise just pass a charset into this
			// constructor and use it in getBytes()
			this.data = data.getBytes("iso-8859-1");
		} catch (UnsupportedEncodingException uex) {
		}
		this.type = type;
	}
	
	public String getContentType() {
		return type;
	}
	
	/**
	 * Return an InputStream for the data.
	 * Note - a new stream must be returned each time.
	 */
	public InputStream getInputStream() throws IOException {
		if (data == null)
			throw new IOException("no data");
		return new ByteArrayInputStream(data);
	}
	
	public String getName() {
		return "dummy";
	}
	
	public OutputStream getOutputStream() throws IOException {
		throw new IOException("cannot do this");
	}
}