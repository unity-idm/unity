/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Size limited {@link OutputStream}. After the size limit is hit, 
 * no more writes are passing through.
 * @author K. Benedyczak
 */
public class LimitedOuputStream extends OutputStream
{
	private OutputStream os;
	private int length;
	private int written = 0;
	private boolean overflow = false;
	
	public LimitedOuputStream(int length, OutputStream os)
	{
		this.length = length;
		this.os = os;
	}

	@Override
	public synchronized void write(int b) throws IOException
	{
		if (written+1 > length)
		{
			overflow = true;
			return;
		}
		written++;
		os.write(b);
	}

	@Override
	public synchronized void write(byte b[], int off, int len) throws IOException
	{
		if (written+len > length)
		{
			overflow = true;
			return;
		}
		written+=len;
		os.write(b, off, len);
	}

	@Override
	public void flush() throws IOException 
	{
		os.flush();
	}

	@Override
	public void close() throws IOException 
	{
		os.close();
	}

	public OutputStream getWrappedStream()
	{
		return os;
	}
	
	public boolean isOverflow()
	{
		return overflow;
	}
	
	public long getLength()
	{
		return written;
	}
}
