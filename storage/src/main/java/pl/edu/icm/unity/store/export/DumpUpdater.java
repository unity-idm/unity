/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.AppDataSchemaVersion;

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
	
	private List<JsonDumpUpdate> updaters;
	
	@Autowired
	public DumpUpdater(List<JsonDumpUpdate> updaters)
	{
		this.updaters = updaters;
		this.updaters.sort((a, b) -> Integer.compare(a.getUpdatedVersion(), b.getUpdatedVersion()));
		int version = updaters.get(0).getUpdatedVersion();
		for (JsonDumpUpdate update: updaters)
		{
			if (update.getUpdatedVersion() != version)
				throw new IllegalStateException(
						"updaters chain is inconsistent: no updater from version " + version);
			version++;
		}
		if (version != AppDataSchemaVersion.CURRENT.getAppSchemaVersion())
			throw new IllegalStateException(
					"updaters chain is incomplete: no updater to current app version");
	}


	public InputStream update(InputStream is, DumpHeader header) throws IOException
	{
		if (header.getVersionMajor() < MIN_SUPPORTED_MAJOR || 
			(header.getVersionMajor() == MIN_SUPPORTED_MAJOR && 
			header.getVersionMinor() < MIN_SUPPORTED_MINOR))
			throw new IOException("Import of data can not be performed from dumps "
					+ "which were created with Unity versions older then 1.9.x. "
					+ "Update from 1.8.0 can work, but is not officially supported.");

		if (header.getVersionMajor() > AppDataSchemaVersion.CURRENT.getAppSchemaVersion())
				throw new IOException("Import of data can not be performed from dumps "
						+ "which were created with Unity versions newer "
						+ "then the current one.");
		
		for (JsonDumpUpdate update: updaters)
		{
			int updateFrom = update.getUpdatedVersion();
			if (header.getVersionMajor() <= updateFrom)
				is = performUpdate(is, update, updateFrom, updateFrom+1);
		}
		return is;
	}

	
	private InputStream performUpdate(InputStream is, JsonDumpUpdate updateImpl,
			int fromVersion, int toVersion) throws IOException
	{
		log.info("Updating database dump from {} --> {}", fromVersion, toVersion);
		return updateImpl.update(is);
	}
}
