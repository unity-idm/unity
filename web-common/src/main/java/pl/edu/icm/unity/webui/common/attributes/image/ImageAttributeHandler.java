/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

/**
 * Image attribute handler for the web
 *
 * @author R. Ledzinski
 */
public class ImageAttributeHandler implements WebAttributeHandler
{
	private UnityMessageSource msg;
	private ImageAttributeSyntax syntax;

	public ImageAttributeHandler(UnityMessageSource msg, AttributeValueSyntax<?> syntax)
	{
		this.msg = msg;
		this.syntax = (ImageAttributeSyntax) syntax;
	}

	@Override
	public String getValueAsString(String value)
	{
		return "Image";
	}

	@Override
	public Component getRepresentation(String valueRaw, AttributeViewerContext context)
	{
		UnityImage value = syntax.convertFromString(valueRaw);
		if (value == null)
			return ImageValueEditor.getErrorImage();

		Resource resValue = new SimpleImageSource(value.getImage(), value.getType()).getResource();

		if (resValue != null)
		{
			Image image = new Image();

			image.setSource(resValue);
			return image;
		} else
		{
			return ImageValueEditor.getErrorImage();
		}
	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new ImageValueEditor(initialValue, label, msg, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new CompactFormLayout(ImageValueEditor.getHints(syntax, msg));
	}
}
