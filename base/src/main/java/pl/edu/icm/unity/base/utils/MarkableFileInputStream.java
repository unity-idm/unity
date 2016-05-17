package pl.edu.icm.unity.base.utils;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;



/**
 * All credits to http://stackoverflow.com/questions/1094703/java-file-input-with-rewind-reset-capability
 * This version is only slightly improved.
 * @author http://stackoverflow.com/users/10026/ykaganovich
 */
public class MarkableFileInputStream extends FilterInputStream
{
	private FileChannel myFileChannel;
	private long mark = -1;

	public MarkableFileInputStream(FileInputStream fis)
	{
		super(fis);
		myFileChannel = fis.getChannel();
	}

	@Override
	public boolean markSupported()
	{
		return true;
	}

	@Override
	public synchronized void mark(int readlimit)
	{
		try
		{
			mark = myFileChannel.position();
		} catch (IOException ex)
		{
			throw new IllegalStateException("Can not set mark", ex);
		}
	}

	@Override
	public synchronized void reset() throws IOException
	{
		if (mark == -1)
			throw new IOException("Stream has not been marked yet");
		myFileChannel.position(mark);
	}
}