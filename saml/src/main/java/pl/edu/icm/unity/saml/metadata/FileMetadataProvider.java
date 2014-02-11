/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;

import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;

/**
 * Returns metadata read from file. The contents is parsed to check syntax. The file is automatically 
 * checked for updates.
 * @author K. Benedyczak
 */
public class FileMetadataProvider implements MetadataProvider
{
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, FileMetadataProvider.class);
	private File file;
	private Date lastModification;
	private ScheduledExecutorService scheduler;
	private EntityDescriptorDocument document;
	
	public FileMetadataProvider(ExecutorsService executorsService, final File file) throws IOException
	{
		this.file = file;
		scheduler = executorsService.getService();
		load();
		scheduler.scheduleWithFixedDelay(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					if (file.lastModified() > lastModification.getTime())
					{
						log.info("Metadata file modification detected, reloading " + file);
						load();
					}
				} catch (IOException e)
				{
					log.error("Can not load the metadata from the configured file " + file, e);
				}
			}
		}, 20, 20, TimeUnit.SECONDS);
	}

	private synchronized void load() throws IOException
	{
		try
		{
			document = EntityDescriptorDocument.Factory.parse(file);
			lastModification = new Date();
		} catch (Exception e)
		{
			throw new IOException("Metadata file can not be loaded", e);
		}
	}
	
	@Override
	public synchronized EntityDescriptorDocument getMetadata()
	{
		return document;
	}

	@Override
	public synchronized Date getLastmodification()
	{
		return lastModification;
	}
}
