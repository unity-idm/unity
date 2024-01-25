/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.ext;



import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.AttributeMetadataEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.webui.common.FormValidationException;


/**
 * Handler for {@link ContactEmailMetadataProvider}. 
 * @author K. Benedyczak
 */
public class ContactEmailMetadataHandler implements WebAttributeMetadataHandler
{
	private final MessageSource msg;
	
	public ContactEmailMetadataHandler(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedMetadata()
	{
		return ContactEmailMetadataProvider.NAME;
	}

	@Override
	public Component getRepresentation(String value)
	{
		return new NativeLabel(msg.getMessage("ContactEmailMetadataHandler.label"));
	}

	@Override
	public AttributeMetadataEditor getEditorComponent(String initialValue)
	{
		return new AttributeMetadataEditor()
		{
			@Override
			public String getValue() throws FormValidationException
			{
				return "";
			}
			
			@Override
			public Component getEditor()
			{
				VerticalLayout ret = new VerticalLayout();
				ret.setSpacing(true);
				ret.setMargin(false);
				ret.add(new Span(msg.getMessage("ContactEmailMetadataHandler.label")));
				ret.add(new Span(" "));
				ret.add(new Span(msg.getMessage("MetadataHandler.noParamsAreNeeded")));
				return ret;
			}
		};
	}
}
