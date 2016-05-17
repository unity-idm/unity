/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Updates a JSON dump before it is actually imported.
 * Changes are performed in JSON contents, input stream is reset after the changes are performed.
 * @author K. Benedyczak
 */
@Component
public class DumpUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, DumpUpdater.class);
	
	private static final int MIN_SUPPORTED_MAJOR = 1;
	private static final int MIN_SUPPORTED_MINOR = 5;
	
	@Autowired 
	private UpdateFrom1_9_x updateFrom1_9_x;
	
	
	public void update(InputStream is, DumpHeader header) throws IOException
	{
		if (header.getVersionMajor() < MIN_SUPPORTED_MAJOR || 
			(header.getVersionMajor() == MIN_SUPPORTED_MAJOR && 
			header.getVersionMinor() < MIN_SUPPORTED_MINOR))
			throw new IOException("Import of data can not be performed from dumps "
					+ "which were created with Unity versions older then 1.9.x. "
					+ "Update from 1.8.0 can work, but is not officially supported.");
		
		if (header.getVersionMajor() < DumpSchemaVersion.V_INITIAL2.getVersionCode())
			performUpdate(is, updateFrom1_9_x, DumpSchemaVersion.V_INITIAL2);
	}

	
	private void performUpdate(InputStream is, Update updateImpl,
			DumpSchemaVersion toVersion) throws IOException
	{
		log.info("Updating database dump from " + toVersion.getPreviousName() + 
				" --> " + toVersion.getName() + " [" + toVersion.getVersionCode() + "]");
		is.mark(-1);
		updateImpl.update(is);
		is.reset();
	}
}
