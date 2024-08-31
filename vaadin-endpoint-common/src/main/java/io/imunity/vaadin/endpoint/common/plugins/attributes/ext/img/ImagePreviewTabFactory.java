/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;

import pl.edu.icm.unity.base.attribute.image.UnityImage;

class ImagePreviewTabFactory
{
	public static void openTab(UnityImage image)
	{

		StreamResource streamResource = new StreamResource("imgattribute-" + UUID.randomUUID() + "." + image.getType()
				.toExt(), () -> new ByteArrayInputStream(image.getImage()));
		Anchor link = new Anchor(streamResource, "");
		link.setTarget("_blank"); 
		UI.getCurrent()
				.add(link);
		UI.getCurrent()
				.getPage()
				.open(link.getHref(), "_blank");
	}

}
