/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.message.MessageSource;

class ImagePreviewDialogFactory
{
	static ConfirmDialog getPreviewDialog(MessageSource msg, UnityImage value)
	{
		ConfirmDialog confirmDialog = new ConfirmDialog();
		confirmDialog.setHeader(msg.getMessage("ImageAttributeHandler.image"));
		confirmDialog.setConfirmText(msg.getMessage("close"));
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value.getImage());
		StreamResource streamResource = new StreamResource("imgattribute-" + UUID.randomUUID() + "." + value.getType().toExt(), () -> byteArrayInputStream);
		Image image = new Image(streamResource, "");
		confirmDialog.add(image);
		confirmDialog.addConfirmListener(event -> confirmDialog.close());
		return confirmDialog;
	}
}
