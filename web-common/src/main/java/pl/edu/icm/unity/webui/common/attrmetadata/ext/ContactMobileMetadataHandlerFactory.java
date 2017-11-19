/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attrmetadata.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.utils.ContactMobileMetadataProvider;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandlerFactory;


@Component
public class ContactMobileMetadataHandlerFactory implements WebAttributeMetadataHandlerFactory
{
	private UnityMessageSource msg;
	
	@Autowired
	public ContactMobileMetadataHandlerFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedMetadata()
	{
		return ContactMobileMetadataProvider.NAME;
	}

	@Override
	public WebAttributeMetadataHandler newInstance()
	{
		return new ContactMobileMetadataHandler(msg);
	}
}
