/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;
import io.imunity.vaadin.elements.ErrorLabel;
import io.imunity.vaadin.elements.InputLabel;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.attr.ImageType;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;

class UnityImageValueComponent extends VerticalLayout implements HasLabel
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, UnityImageValueComponent.class);
	static final int PREVIEW_WIDTH = 256;
	static final int PREVIEW_HEIGHT = 128;
	
	private final InputLabel label;
	private final Image image;
	private final Upload upload;
	private final ErrorLabel error;
	private final MessageSource msg;

	private UnityImage value;
	
	UnityImageValueComponent(UnityImage initialValue, ImageConfiguration imgConfig, MessageSource msg)
	{
		this.msg = msg;
		this.value = initialValue;
		this.label = new InputLabel("");
		this.label.setVisible(false);

		error = new ErrorLabel("");
		error.setVisible(false);

		image = new Image();
		image.addClickListener(event -> getConfirmDialog().open());

		if (value != null)
			showValue();

		Checkbox scale = new Checkbox();
		HorizontalLayout scaleLayout = new HorizontalLayout(scale, new Label(msg.getMessage("ImageAttributeHandler.scaleIfNeeded")));
		scaleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		scaleLayout.getStyle().set("gap", "0.5em");
		scaleLayout.getStyle().set("margin-top", "0.5em");
		scaleLayout.getStyle().set("margin-bottom", "0.5em");
		scale.setValue(true);

		MemoryBuffer memoryBuffer = new MemoryBuffer();
		upload = new Upload(memoryBuffer);
		upload.setAcceptedFileTypes(ImageType.getSupportedMimeTypes(","));
		upload.setMaxFileSize(imgConfig.getMaxSize());
		upload.addSucceededListener(event ->
		{
			FileData fileData1 = memoryBuffer.getFileData();
			setUnityImageValue(new UnityImage(((ByteArrayOutputStream)fileData1.getOutputBuffer()).toByteArray(), ImageType.fromMimeType(fileData1.getMimeType())));
			showValue();
		});
		upload.getElement().addEventListener("file-remove", e -> cleanImage());
		upload.addFileRejectedListener(event -> showErrorNotification(event.getErrorMessage()));
		upload.addFailedListener(event -> showErrorNotification(event.getReason().getMessage()));
		upload.addStartedListener(e -> setNormalMode());

		setPadding(false);
		setMargin(false);
		getStyle().set("gap", "0");
		add(label, image, upload, error, scaleLayout, getHints(imgConfig, msg));
	}

	void addChangeListener(Runnable runnable)
	{
		upload.addStartedListener(e -> runnable.run());
	}

	private void cleanImage()
	{
		image.setSrc("");
		image.setVisible(false);
		image.getElement().setProperty("title", null);
	}

	private void setErrorMode()
	{
		cleanImage();
		upload.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
		error.setVisible(true);
		label.setErrorMode();
	}

	private void setNormalMode()
	{
		upload.getStyle().set("background-color", "inherit");
		label.setNormalMode();
		error.setVisible(false);
	}

	private void showErrorNotification(String txt)
	{
		Notification notification = Notification.show(txt, 5000, Notification.Position.MIDDLE);
		notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
	}

	private void showValue()
	{
		try
		{
			image.setVisible(true);
			byte[] scaledDownData = value.getScaledDownImage(PREVIEW_WIDTH, PREVIEW_HEIGHT);
			UnityImage scaledDown = new UnityImage(scaledDownData, value.getType());

			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(scaledDown.getImage());
			StreamResource streamResource = new StreamResource("imgattribute-" + UUID.randomUUID() + "." + scaledDown.getType().toExt(), () -> byteArrayInputStream);
			image.setSrc(streamResource);
			error.setVisible(false);
			image.setVisible(true);
			image.getElement().setProperty("title", msg.getMessage("ImageAttributeHandler.clickToEnlarge"));
		} catch (Exception e)
		{
			LOG.warn("Problem getting value's image as resource for editing: " + e, e);
			cleanImage();
			showErrorNotification(e.getMessage());
		}
	}

	private ConfirmDialog getConfirmDialog()
	{
		ConfirmDialog confirmDialog = new ConfirmDialog();
		confirmDialog.setHeader(msg.getMessage("ImageAttributeHandler.image"));
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value.getImage());
		StreamResource streamResource = new StreamResource("imgattribute-" + UUID.randomUUID() + "." + value.getType().toExt(), () -> byteArrayInputStream);
		Image image = new Image(streamResource, "");
		confirmDialog.add(image);
		confirmDialog.addConfirmListener(event -> confirmDialog.close());
		confirmDialog.setSizeFull();
		return confirmDialog;
	}

	private void setUnityImageValue(UnityImage value)
	{
		error.setVisible(false);
		this.value = value;
	}
	
	Optional<UnityImage> getValue(boolean required, ImageValidator validator) throws IllegalAttributeValueException
	{
		if (value == null && !required)
			return Optional.empty();
		if (value == null)
		{
			error.setText(msg.getMessage("ImageAttributeHandler.noImage"));
			setErrorMode();
			throw new IllegalAttributeValueException(msg.getMessage("ImageAttributeHandler.noImage"));
		}
		try
		{
			validator.validate(value);
		} catch (IllegalAttributeValueException e)
		{
			error.setText(e.getMessage());
			setErrorMode();
			throw e;
		}

		error.setVisible(false);
		image.setVisible(true);
		return Optional.of(value);
	}

	public void setLabel(String label) {
		this.label.setVisible(true);
		this.label.setText(label);
	}

	public void setRequired(boolean required) {
		this.label.setRequired(required);
	}

	public String getLabel() {
		return label.getText();
	}

	static Component getErrorImage()
	{
		return VaadinIcon.EXCLAMATION_CIRCLE_O.create();
	}

	static Component getHints(ImageConfiguration imgConfig, MessageSource msg)
	{
		VerticalLayout verticalLayout = new VerticalLayout(
				new Label(msg.getMessage("ImageAttributeHandler.maxSize", imgConfig.getMaxSize() / 1024)),
				new Label(msg.getMessage("ImageAttributeHandler.maxDim", imgConfig.getMaxWidth(), imgConfig.getMaxHeight()))
		);
		verticalLayout.setMargin(false);
		verticalLayout.setPadding(false);
		verticalLayout.getStyle().set("gap", "0");
		return verticalLayout;
	}
}