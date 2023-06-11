/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attrmetadata.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandlerFactory;


/**
 * Factory for {@link EntityNameMetadataHandler}.
 * @author K. Benedyczak
 */
@Component
public class EntityNameMetadataHandlerFactory implements WebAttributeMetadataHandlerFactory
{
	private MessageSource msg;
	
	@Autowired
	public EntityNameMetadataHandlerFactory(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedMetadata()
	{
		return EntityNameMetadataProvider.NAME;
	}

	@Override
	public WebAttributeMetadataHandler newInstance()
	{
		return new EntityNameMetadataHandler(msg);
	}
}
