/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.BaseImageAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.UnityImageSpec;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

/**
 * Image attribute handler for the web
 *
 * @author R. Ledzinski
 */
class BaseImageAttributeHandler<T extends UnityImageSpec> implements WebAttributeHandler
{
	private UnityMessageSource msg;
	private BaseImageAttributeSyntax<T> syntax;

	BaseImageAttributeHandler(UnityMessageSource msg, BaseImageAttributeSyntax<T> syntax)
	{
		this.msg = msg;
		this.syntax = syntax;
	}

	@Override
	public String getValueAsString(String value)
	{
		return "Image";
	}

	@Override
	public Component getRepresentation(String valueRaw, AttributeViewerContext context)
	{
		T value = syntax.convertFromString(valueRaw);
		if (value == null)
			return BaseImageValueEditor.getErrorImage();

		if (context.isScaleImage())
		{
			
			value.scaleDown(context.getImageScaleWidth(), context.getImageScaleHeight());
		}

		Resource resValue = new SimpleImageSource(value).getResource();

		if (resValue != null)
		{
			Image image = new Image();
			image.setSource(resValue);
	
			if (context.isCustomWidth() && !context.isScaleImage())
			{

				if (context.getCustomWidth() > 0)
				{
					image.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
				} else
				{
					image.setWidthUndefined();
				}
			}
			if (context.isCustomHeight() && !context.isScaleImage())
			{
				if (context.getCustomHeight() > 0)
				{
					image.setHeight(context.getCustomHeight(), context.getCustomHeightUnit());
				}

				else
				{
					image.setHeightUndefined();
				}
			}

			return image;
		} else
		{
			return BaseImageValueEditor.getErrorImage();
		}
	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new BaseImageValueEditor<>(initialValue, label, msg, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new CompactFormLayout(BaseImageValueEditor.getHints(syntax, msg));
	}
}
