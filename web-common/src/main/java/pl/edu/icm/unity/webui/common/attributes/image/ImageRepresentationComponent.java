/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;

class ImageRepresentationComponent extends CustomComponent
{
	ImageRepresentationComponent(UnityImage value, AttributeViewerContext context)
	{
		this(value, context, null);
	}

	ImageRepresentationComponent(UnityImage value, AttributeViewerContext context, String linkURL)
	{
		setCompositionRoot(getRootComponent(value, context,linkURL));
	}

	private Component getRootComponent(UnityImage value, AttributeViewerContext context, String linkURL)
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(false);
		layout.setSpacing(false);
		layout.addComponent(getRepresentation(value, context, linkURL));
		return layout;
	}

	private Component getRepresentation(UnityImage value, AttributeViewerContext context, String linkURL)
	{
		if (value == null)
			return UnityImageValueComponent.getErrorImage();

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
				} else
				{
					image.setHeightUndefined();
				}
			}
			
			if (linkURL != null && context.isScaleImage())
			{
				Link link = new Link();
				link.setTargetName("_blank");
				link.setIcon(image.getSource());
				link.setResource(new ExternalResource(linkURL));
				return link;
			}
			
			return image;
		} else
		{
			return UnityImageValueComponent.getErrorImage();
		}
	}
}
