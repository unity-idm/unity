/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import io.imunity.vaadin.elements.InputLabel;
import io.imunity.vaadin.endpoint.common.HtmlTooltipAttacher;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.message.MessageSource;

import java.io.ByteArrayInputStream;
import java.util.UUID;

class ImageRepresentationComponent extends VerticalLayout implements HasLabel
{
	private final Span label;
	private final MessageSource msg;
	
	ImageRepresentationComponent(MessageSource msg, UnityImage value, AttributeViewerContext context)
	{
		this(msg, value, context, null);
	}

	ImageRepresentationComponent(MessageSource msg, UnityImage value, AttributeViewerContext context, String linkURL)
	{
		this.msg = msg;
		add(getRepresentation(value, context, linkURL));
		this.label = new InputLabel("");
		addComponentAsFirst(label);
		setPadding(false);
		setSpacing(false);
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
		Image image = getImage(context, streamResource);
		image.addClickListener(event -> ImagePreviewDialogFactory.getPreviewDialog(msg, value).open());

		HtmlTooltipAttacher.to(image, msg.getMessage("ImageAttributeHandler.clickToEnlarge"));
		if(context.getBorderRadius() != null && context.getBorderUnit() != null)
			image.getStyle().set("border-radius", context.getBorderRadius() + context.getBorderUnit().getSymbol());
		if (linkURL != null && context.isScaleImage())
		{
			Image externalImage = new Image(linkURL, "");
			Anchor anchor = new Anchor(linkURL, externalImage);
			anchor.setTarget("_blank");
			return anchor;
		}
			return image;
	}

	private static Image getImage(AttributeViewerContext context, StreamResource streamResource)
	{
		Image image = new Image(streamResource, "");
		if (context.isCustomWidth() && !context.isScaleImage() && !context.isCustomMaxWidth())
		{
				if (!context.isCustomWidthAsString())
				{
					if (context.getCustomWidth() > 0)
					{
						image.getElement().getStyle().set("width", context.getCustomWidth() + context.getCustomWidthUnit().getSymbol());
					} else
					{
						image.getElement().getStyle().set("width", "unset");
					}
				}else 
				{
					image.getElement().getStyle().set("width", context.getCustomWidthAsString());
				}
		}
		if (context.isCustomHeight() && !context.isScaleImage() && !context.isCustomMaxHeight())
		{
			if (context.getCustomHeight() > 0)
			{
				image.setHeight(context.getCustomHeight() + context.getCustomHeightUnit().getSymbol());
			} else
			{
				image.setHeight("unset");
			}
		}
		
		
		if (context.isCustomMaxWidth())
		{
			if (context.getCustomMaxWidth() > 0)
			{
				image.setMaxWidth(context.getCustomMaxWidth() + context.getCustomMaxWidthUnit().getSymbol());
			} else
			{
				image.setMaxWidth("unset");
			}
		}
		
		if (context.isCustomMaxHeight())
		{
			if (context.getCustomMaxHeight() > 0)
			{
				image.setMaxHeight(context.getCustomMaxHeight() + context.getCustomMaxHeightUnit().getSymbol());
			} else
			{
				image.setMaxHeight("unset");
			}
		}
		
		return image;
	}

	@Override
	public void setLabel(String label) {
		this.label.setText(label);
	}

	@Override
	public String getLabel() {
		return label.getText();
	}
}
