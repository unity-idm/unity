/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;

import pl.edu.icm.unity.base.file.FileData;

/**
 * 
 * @author P.Piernik
 *
 */
public class FileStreamResource implements StreamSource
{
	private final byte[] isData;

	public FileStreamResource(byte[] isData)
	{
		this.isData = isData;
	}
	
	public FileStreamResource(FileData fileData)
	{
		this.isData = fileData.contents;
	}

	@Override
	public InputStream getStream()
	{
		return new ByteArrayInputStream(isData);
	}

	public Resource getResource()
	{
		return new StreamResource(this, UUID.randomUUID().toString());
	}
}
