/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.*;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ext.AttributeHandlerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.attr.LinkableImage;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;

import java.net.URL;

class PublicLinkableImageAttributeHandler implements WebAttributeHandler
{
	private final MessageSource msg;
	private final PublicLinkableImageSyntax syntax;

	PublicLinkableImageAttributeHandler(MessageSource msg, PublicLinkableImageSyntax syntax)
	{
		this.msg = msg;
		this.syntax = syntax;
	}

	@Override
	public String getValueAsString(String value)
	{
		return PublicLinkableImageSyntax.ID;
	}

	@Override
	public Component getRepresentation(String valueRaw, AttributeViewerContext context)
	{
		LinkableImage value = syntax.convertFromString(valueRaw);
		if (value.getUnityImage() != null)
		{
			String linkURL = syntax.getImageUrl(value);
			return new ImageRepresentationComponent(value.getUnityImage(), context, linkURL);
		}
		if (value.getUrl() != null)
			return AttributeHandlerHelper.getRepresentation(value.getUrl().toExternalForm(), context);
		return AttributeHandlerHelper.getRepresentation("", context);
	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new PublicLinkableImageValueEditor(initialValue, label, msg, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new VerticalLayout(UnityImageValueComponent.getHints(syntax.getConfig(), msg));
	}

	static class PublicLinkableImageValueEditor implements AttributeValueEditor
	{
		private final PublicLinkableImageSyntax syntax;
		private final PublicLinkableImageValueComponent valueComponent;
		private boolean required;

		PublicLinkableImageValueEditor(String valueRaw,
		                               String label,
		                               MessageSource msg,
		                               PublicLinkableImageSyntax syntax)
		{
			LinkableImage value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			valueComponent = new PublicLinkableImageValueComponent(value, syntax.getConfig(), msg);
			valueComponent.setLabel(label);
			this.syntax = syntax;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			required = context.isRequired();
			valueComponent.setRequiredIndicatorVisible(required);
			return new ComponentsContainer(valueComponent);
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			return valueComponent.getValue(required, syntax)
					.map(syntax::convertToString)
					.orElse(null);
		}

		@Override
		public void setLabel(String label)
		{
			valueComponent.setLabel(label);
		}
	}

	@org.springframework.stereotype.Component
	static class PublicLinkableImageAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;
		private final URL serverAdvertisedAddress;

		@Autowired
		PublicLinkableImageAttributeHandlerFactory(MessageSource msg,
		                                           AdvertisedAddressProvider advertisedAddressProvider)
		{
			this.msg = msg;
			this.serverAdvertisedAddress = advertisedAddressProvider.get();
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return PublicLinkableImageSyntax.ID;
		}

		@Override
		public AttributeSyntaxEditor<LinkableImage> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
		{
			return new BaseImageSyntaxEditor<>((PublicLinkableImageSyntax) initialValue,
					() -> new PublicLinkableImageSyntax(serverAdvertisedAddress), msg);
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new PublicLinkableImageAttributeHandler(msg, (PublicLinkableImageSyntax) syntax);
		}
	}
}
