/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;

class ImageRepresentationComponent extends CustomComponent
{
	ImageRepresentationComponent(UnityImage value, AttributeViewerContext context)
	{
		setCompositionRoot(getRootComponent(value, context));
	}

	private Component getRootComponent(UnityImage value, AttributeViewerContext context)
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(false);
		layout.setSpacing(false);
		layout.addComponent(getRepresentation(value, context));
		return layout;
	}

	private Component getRepresentation(UnityImage value, AttributeViewerContext context)
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
			return image;
		} else
		{
			return UnityImageValueComponent.getErrorImage();
		}
	}
}
