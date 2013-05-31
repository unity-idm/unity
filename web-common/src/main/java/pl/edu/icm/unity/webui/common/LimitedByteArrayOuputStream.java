/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Size limited {@link ByteArrayOutputStream}. After the size limit is hit, 
 * no more writes are allowed.
 * @author K. Benedyczak
 */
public class LimitedByteArrayOuputStream extends OutputStream
{
	private ByteArrayOutputStream bos;
	private int length;
	private boolean overflow = false;
	
	public LimitedByteArrayOuputStream(int length)
	{
		this.length = length;
		bos = new ByteArrayOutputStream(length > 102400 ? 102400 : length);
	}

	@Override
	public synchronized void write(int b) throws IOException
	{
		if (bos.size()+1 > length)
		{
			overflow = true;
			return;
		}
		bos.write(b);
	}

	@Override
	public synchronized void write(byte b[], int off, int len) throws IOException
	{
		if (bos.size()+len > length)
		{
			overflow = true;
			return;
		}
		bos.write(b, off, len);
	}
	
	public synchronized byte[] toByteArray()
	{
		return bos.toByteArray();
	}
	
	public boolean isOverflow()
	{
		return overflow;
	}
}
