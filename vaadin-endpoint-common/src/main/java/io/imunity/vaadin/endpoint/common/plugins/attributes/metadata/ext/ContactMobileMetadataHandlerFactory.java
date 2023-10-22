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
import pl.edu.icm.unity.stdext.utils.ContactMobileMetadataProvider;



@Component
public class ContactMobileMetadataHandlerFactory implements WebAttributeMetadataHandlerFactory
{
	private MessageSource msg;
	
	@Autowired
	public ContactMobileMetadataHandlerFactory(MessageSource msg)
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
