/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.message.MessageSource;

class ImagePreviewDialogFactory
{
	static Dialog getPreviewDialog(MessageSource msg, UnityImage value)
	{
		Dialog confirmDialog = new Dialog();
		confirmDialog.setResizable(true);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value.getImage());
		StreamResource streamResource = new StreamResource("imgattribute-" + UUID.randomUUID() + "." + value.getType()
				.toExt(), () -> byteArrayInputStream);
		Image image = new Image(streamResource, "");
		image.setWidth(value.getWidth(), Unit.PIXELS);
		image.setHeight(value.getHeight(), Unit.PIXELS);
		confirmDialog.add(image);
		VerticalLayout dialogLayout = createDialogLayout(confirmDialog, msg, image);
		confirmDialog.add(dialogLayout);
		return confirmDialog;
	}

	private static VerticalLayout createDialogLayout(Dialog dialog, MessageSource msg, Image image)
	{
		H3 headline = new H3(msg.getMessage("ImageAttributeHandler.image"));
		Button closeButton = new Button(msg.getMessage("close"));
		closeButton.addClickListener(e -> dialog.close());
		VerticalLayout dialogLayout = new VerticalLayout(headline, image, closeButton);
		dialogLayout.setSizeFull();
		dialogLayout.setPadding(false);
		dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
		dialogLayout.setAlignSelf(FlexComponent.Alignment.END, closeButton);
		return dialogLayout;
	}
}
