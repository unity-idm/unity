/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.*;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.BaseImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;


class ImageAttributeHandler implements WebAttributeHandler
{
	private final MessageSource msg;
	private final BaseImageAttributeSyntax<UnityImage> syntax;

	ImageAttributeHandler(MessageSource msg, BaseImageAttributeSyntax<UnityImage> syntax)
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
		UnityImage value = syntax.convertFromString(valueRaw);
		return new ImageRepresentationComponent(value, context);
	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new UnityImageValueEditor(initialValue, label, msg, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new VerticalLayout(UnityImageValueComponent.getHints(syntax.getConfig(), msg));
	}

	@org.springframework.stereotype.Component
	static class ImageAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;

		@Autowired
		ImageAttributeHandlerFactory(MessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return ImageAttributeSyntax.ID;
		}

		@Override
		public AttributeSyntaxEditor<UnityImage> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
		{
			return new BaseImageSyntaxEditor<>((ImageAttributeSyntax) initialValue, ImageAttributeSyntax::new, msg);
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new ImageAttributeHandler(msg, (ImageAttributeSyntax) syntax);
		}
	}

}
