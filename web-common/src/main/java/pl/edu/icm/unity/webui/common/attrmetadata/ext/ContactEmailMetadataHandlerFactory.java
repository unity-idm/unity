/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attrmetadata.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandlerFactory;


/**
 * Factory for {@link ContactEmailMetadataHandler}
 * @author K. Benedyczak
 */
@Component
public class ContactEmailMetadataHandlerFactory implements WebAttributeMetadataHandlerFactory
{
	private UnityMessageSource msg;
	
	@Autowired
	public ContactEmailMetadataHandlerFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedMetadata()
	{
		return ContactEmailMetadataProvider.NAME;
	}

	@Override
	public WebAttributeMetadataHandler newInstance()
	{
		return new ContactEmailMetadataHandler(msg);
	}
}
