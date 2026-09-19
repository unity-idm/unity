/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import java.util.UUID;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.streams.DownloadHandler;

import io.imunity.vaadin.endpoint.common.file.DownloadHandlers;
import pl.edu.icm.unity.base.attribute.image.UnityImage;

class ImagePreviewTabFactory
{
	public static void openTab(UnityImage image)
	{
		DownloadHandler handler = DownloadHandlers.forUnityImage(image,
			"imgattribute-" + UUID.randomUUID() + "." + image.getType().toExt());

		Anchor link = new Anchor(handler, "");
		link.setTarget("_blank"); 
		UI.getCurrent()
				.add(link);
		UI.getCurrent()
				.getPage()
				.open(link.getHref(), "_blank");
	}

}
