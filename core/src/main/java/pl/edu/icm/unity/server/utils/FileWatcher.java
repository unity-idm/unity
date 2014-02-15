package pl.edu.icm.unity.server.utils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Performs an action when a file change is detected. This should be executed
 * periodically, for example using a scheduled executor service.
 * 
 * @author schuller
 */
public class FileWatcher implements Runnable
{
	private final File target;

	private final Runnable action;

	private long lastAccessed;

	public FileWatcher(File target, Runnable action) throws FileNotFoundException
	{
		if (!target.exists() || !target.canRead())
		{
			throw new FileNotFoundException("File " + target.getAbsolutePath()
					+ " does not exist or is not readable.");
		}
		this.target = target;
		this.action = action;
		lastAccessed = target.lastModified();
	}

	/**
	 * Check if target file has been touched and invoke the action if it has been.
	 */
	public void run()
	{
		if (target.lastModified() > lastAccessed)
		{
			lastAccessed = target.lastModified();
			action.run();
		}
	}
}
