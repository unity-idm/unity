/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.WebAttributeMetadataHandler;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.WebAttributeMetadataHandlerFactory;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;


/**
 * Factory for {@link ContactEmailMetadataHandler}
 * @author K. Benedyczak
 */
@Component
public class ContactEmailMetadataHandlerFactory implements WebAttributeMetadataHandlerFactory
{
	private final MessageSource msg;
	
	@Autowired
	public ContactEmailMetadataHandlerFactory(MessageSource msg)
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
