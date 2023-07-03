/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.attribute.image.ImageType;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import java.util.Optional;

interface ImageValidator
{
	void validate(UnityImage value) throws IllegalAttributeValueException;
}

class UnityImageValueComponent extends CustomComponent
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, UnityImageValueComponent.class);
	static final int PREVIEW_WIDTH = 256;
	static final int PREVIEW_HEIGHT = 128;
	
	private final Image field;
	private final Upload upload;
	private final ProgressBar progressIndicator;
	private final CheckBox scale;
	private final Label error;
	private final MessageSource msg;

	private UnityImage value;
	
	UnityImageValueComponent(UnityImage initialValue,
			ImageConfiguration imgConfig,
			MessageSource msg)
	{
		this.msg = msg;
		this.value = initialValue;
		
		error = new Label();
		error.setStyleName(Styles.error.toString());
		error.setVisible(false);

		Label errorImage = getErrorImage();
		errorImage.setVisible(false);

		field = new Image();
		boolean hasValue = value != null;
		field.setVisible(hasValue);
		if (hasValue)
		{
			try
			{
				byte[] scaledDownData = value.getScaledDownImage(PREVIEW_WIDTH, PREVIEW_HEIGHT);
				UnityImage scaledDown = new UnityImage(scaledDownData, value.getType());
				field.setSource(new SimpleImageSource(scaledDown).getResource());
				errorImage.setVisible(false);
				field.setVisible(true);
			} catch (Exception e)
			{
				LOG.warn("Problem getting value's image as resource for editing: {0}", e);
				errorImage.setVisible(true);
				field.setVisible(false);
			}
		}

		field.addClickListener(event ->
		{
			if (hasValue)
				new ShowImageDialog(value, msg).show();
		});

		field.setDescription(msg.getMessage("ImageAttributeHandler.clickToEnlarge"));

		upload = new Upload();
		progressIndicator = new ProgressBar(0);
		progressIndicator.setVisible(false);

		scale = new CheckBox(msg.getMessage("ImageAttributeHandler.scaleIfNeeded"));
		scale.setValue(true);

		UnityImageUploader uploader = new UnityImageUploader(field, upload, imgConfig, msg, progressIndicator, scale,
				this::setUnityImageValue);
		uploader.register();
		upload.setAcceptMimeTypes(ImageType.getSupportedMimeTypes(","));

		
		VerticalLayout layout = new VerticalLayout(field, errorImage, error, upload, progressIndicator, scale,
				getHints(imgConfig, msg));
		layout.setMargin(false);
		setCompositionRoot(layout);
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
			error.setValue(msg.getMessage("ImageAttributeHandler.noImage"));
			error.setVisible(true);
			field.setVisible(false);
			throw new IllegalAttributeValueException(msg.getMessage("ImageAttributeHandler.noImage"));
		}
		try
		{
			validator.validate(value);
		} catch (IllegalAttributeValueException e)
		{
			error.setValue(e.getMessage());
			error.setVisible(true);
			field.setVisible(false);
			throw e;
		}

		error.setVisible(false);
		field.setVisible(true);
		return Optional.of(value);
	}

	static Label getErrorImage()
	{
		Label errorImage = new Label(Images.error.getHtml());
		errorImage.setContentMode(ContentMode.HTML);
		errorImage.addStyleName(Styles.largeIcon.toString());
		return errorImage;
	}

	static Component getHints(ImageConfiguration imgConfig, MessageSource msg)
	{
		Label ret = new Label(msg.getMessage("ImageAttributeHandler.maxSize", imgConfig.getMaxSize() / 1024) + "  "
				+ msg.getMessage("ImageAttributeHandler.maxDim", imgConfig.getMaxWidth(), imgConfig.getMaxHeight()));
		ret.addStyleName(Styles.vLabelSmall.toString());
		return ret;
	}
	
	/**
	 * Simple dialog showing image in it's default size.
	 */
	private class ShowImageDialog extends AbstractDialog
	{
		private UnityImage image;

		public ShowImageDialog(UnityImage image, MessageSource msg)
		{
			super(msg, msg.getMessage("ImageAttributeHandler.image"), msg.getMessage("close"));
			this.image = image;
			setSizeMode(SizeMode.LARGE);
		}

		@Override
		protected Component getContents() throws Exception
		{
			Image imageC = new Image();
			imageC.setSource(new SimpleImageSource(image).getResource());
			return imageC;
		}

		@Override
		protected void onConfirm()
		{
			close();
		}
	}
}
