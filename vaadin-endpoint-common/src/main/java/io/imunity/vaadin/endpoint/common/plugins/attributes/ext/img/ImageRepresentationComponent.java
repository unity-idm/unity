/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import pl.edu.icm.unity.attr.UnityImage;

import java.io.ByteArrayInputStream;
import java.util.UUID;

class ImageRepresentationComponent extends VerticalLayout
{
	ImageRepresentationComponent(UnityImage value, AttributeViewerContext context)
	{
		this(value, context, null);
	}

	ImageRepresentationComponent(UnityImage value, AttributeViewerContext context, String linkURL)
	{
		add(getRepresentation(value, context, linkURL));
	}

	private Component getRepresentation(UnityImage value, AttributeViewerContext context, String linkURL)
	{
		if (value == null)
			return UnityImageValueComponent.getErrorImage();

		if (context.isScaleImage())
		{
			value.scaleDown(context.getImageScaleWidth(), context.getImageScaleHeight());
		}

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value.getImage());
		StreamResource streamResource = new StreamResource("imgattribute-" + UUID.randomUUID() + "." + value.getType().toExt(), () -> byteArrayInputStream);
		Image image = new Image(streamResource, "");
		if (context.isCustomWidth() && !context.isScaleImage())
		{
			if (context.getCustomWidth() > 0)
			{
				image.setWidth(context.getCustomWidth() + context.getCustomWidthUnit().getSymbol());
			} else
			{
				image.setWidth("unset");
			}
		}
		if (context.isCustomHeight() && !context.isScaleImage())
		{
			if (context.getCustomHeight() > 0)
			{
				image.setHeight(context.getCustomHeight() + context.getCustomHeightUnit().getSymbol());
			} else
			{
				image.setHeight("unset");
			}
		}
		if (linkURL != null && context.isScaleImage())
		{
			Image externalImage = new Image(linkURL, "");
			Anchor anchor = new Anchor(linkURL, externalImage);
			anchor.setTarget("_blank");
			return anchor;
		}
			return image;
	}
}
