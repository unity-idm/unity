/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.ext;



import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.AttributeMetadataEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.utils.SensitiveAttributeMetadataProvider;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

public class SensitiveAttributeMetadataHandler implements WebAttributeMetadataHandler
{
	private final MessageSource msg;
	
	public SensitiveAttributeMetadataHandler(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedMetadata()
	{
		return SensitiveAttributeMetadataProvider.NAME;
	}

	@Override
	public Component getRepresentation(String value)
	{
		return new NativeLabel(msg.getMessage("SensitiveAttributeMetadataHandler.label"));
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
				ret.add(new NativeLabel(msg.getMessage("SensitiveAttributeMetadataHandler.label")));
				ret.add(new NativeLabel(" "));
				ret.add(new NativeLabel(msg.getMessage("MetadataHandler.noParamsAreNeeded")));
				return ret;
			}
		};
	}
}
