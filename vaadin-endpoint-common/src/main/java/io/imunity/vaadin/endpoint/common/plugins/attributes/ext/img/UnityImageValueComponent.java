/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
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
import io.imunity.vaadin.endpoint.common.HtmlTooltipAttacher;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeModyficationEvent;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.attribute.image.ImageType;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;

class UnityImageValueComponent extends VerticalLayout implements HasLabel
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, UnityImageValueComponent.class);
	
	private final InputLabel label;
	private final Image image;
	private final Upload upload;
	private final ErrorLabel error;
	private final MessageSource msg;

	private UnityImage value;
	private VerticalLayout uploadNewLayout;
	private VerticalLayout clearImageLayout;
	
	UnityImageValueComponent(UnityImage initialValue, ImageConfiguration imgConfig, MessageSource msg)
	{
		this.msg = msg;
		this.value = initialValue;
		this.label = new InputLabel("");
		this.label.setVisible(false);

		error = new ErrorLabel("");
		error.setVisible(false);

		image = new Image();
		image.addClickListener(event -> ImagePreviewTabFactory.openTab(value));

		
		Checkbox scale = new Checkbox();
		HorizontalLayout scaleLayout = new HorizontalLayout(scale, new Span(msg.getMessage("ImageAttributeHandler.scaleIfNeeded")));
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
			UnityImage image = new UnityImage(((ByteArrayOutputStream)fileData1.getOutputBuffer()).toByteArray(), ImageType.fromMimeType(fileData1.getMimeType()));
			if (scale.getValue())
				image.scaleDown(imgConfig.getMaxWidth(), imgConfig.getMaxHeight());
			setUnityImageValue(image);
			showValue();
			switchView();
			WebSession.getCurrent().getEventBus().fireEvent(new AttributeModyficationEvent());
		});
		upload.getElement().addEventListener("file-remove", e -> cleanImage());
		upload.addFileRejectedListener(event -> showErrorNotification(event.getErrorMessage()));
		upload.addFailedListener(event -> showErrorNotification(event.getReason().getMessage()));
		upload.addStartedListener(e -> setNormalMode());

		setPadding(false);
		setMargin(false);
		getStyle().set("gap", "0");
		add(label, image);
		uploadNewLayout = new VerticalLayout();
		uploadNewLayout.setMargin(false);
		uploadNewLayout.setPadding(false);
		uploadNewLayout.getStyle().set("gap", "0");
		uploadNewLayout.add( upload, error, scaleLayout, getHints(imgConfig, msg));
		clearImageLayout = new VerticalLayout();
		Icon reaupload = new Icon(VaadinIcon.CLOSE_SMALL);
		reaupload.setTooltipText(msg.getMessage("ImageAttributeHandler.removeImage"));
		clearImageLayout.add(reaupload);
		clearImageLayout.setPadding(false);
		reaupload.addClickListener(e -> {
			switchView();
			setUnityImageValue(null);
			cleanImage();
			upload.clearFileList();
		});
		clearImageLayout.setVisible(false);
		
		add(uploadNewLayout);
		add(clearImageLayout);
		
		if (value != null)
		{
			switchView();
			showValue();
		}
	}

	private void switchView()
	{
		uploadNewLayout.setVisible(!uploadNewLayout.isVisible());
		clearImageLayout.setVisible(!clearImageLayout.isVisible());
	}
	
	void addChangeListener(Runnable runnable)
	{
		upload.addStartedListener(e -> runnable.run());
	}

	private void cleanImage()
	{
		image.setSrc("");
		image.setVisible(false);
		WebSession.getCurrent().getEventBus().fireEvent(new AttributeModyficationEvent());

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
		WebSession.getCurrent().getEventBus().fireEvent(new AttributeModyficationEvent());
		Notification notification = Notification.show(txt, 5000, Notification.Position.MIDDLE);
		notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
	}

	private void showValue()
	{
		try
		{
			image.setVisible(true);
			UnityImage scaledDown = new UnityImage(value.getImage(), value.getType());
			StreamResource streamResource = new StreamResource(
					"imgattribute-" + UUID.randomUUID() + "." + scaledDown.getType()
							.toExt(),
					() -> new ByteArrayInputStream(scaledDown.getImage()));
			image.setSrc(streamResource);
			error.setVisible(false);
			image.setVisible(true);
			HtmlTooltipAttacher.to(image,
					msg.getMessage("ImageAttributeHandler.clickToEnlarge"));
			
		} catch (Exception e)
		{
			LOG.warn("Problem getting value's image as resource for editing: " + e, e);
			cleanImage();
			showErrorNotification(e.getMessage());
		}
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
				new Span(msg.getMessage("ImageAttributeHandler.maxSize", imgConfig.getMaxSize() / 1024)),
				new Span(msg.getMessage("ImageAttributeHandler.maxDim", imgConfig.getMaxWidth(), imgConfig.getMaxHeight()))
		);
		verticalLayout.setMargin(false);
		verticalLayout.setPadding(false);
		verticalLayout.getStyle().set("gap", "0");
		verticalLayout.getStyle().set("font-size", "var(--lumo-font-size-xxs)");

		return verticalLayout;
	}
	
	public void setContext(AttributeEditContext context)
	{
		if (context.isCustomMaxWidth())
		{
			image.setMaxWidth(context.getCustomMaxWidth() + context.getCustomMaxWidthUnit().getSymbol());
		}
		
		if (context.isCustomMaxHeight())
		{
			image.setMaxHeight(context.getCustomMaxHeight() + context.getCustomMaxHeightUnit().getSymbol());
		}
	}
	
}
