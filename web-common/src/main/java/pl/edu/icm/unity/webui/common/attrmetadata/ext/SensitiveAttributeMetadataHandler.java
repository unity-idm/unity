/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attrmetadata.ext;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.utils.SensitiveAttributeMetadataProvider;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataEditor;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandler;

public class SensitiveAttributeMetadataHandler implements WebAttributeMetadataHandler
{
	private MessageSource msg;
	
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
		return new Label(msg.getMessage("SensitiveAttributeMetadataHandler.label"));
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
				ret.addComponent(new Label(msg.getMessage("SensitiveAttributeMetadataHandler.label")));
				ret.addComponent(new Label(" "));
				ret.addComponent(new Label(msg.getMessage("MetadataHandler.noParamsAreNeeded")));
				return ret;
			}
		};
	}
}
