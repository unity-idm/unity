/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webui.common.attributes.image;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;
import pl.edu.icm.unity.stdext.utils.ImageType;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

interface ImageValidator
{
	void validate(UnityImage value) throws IllegalAttributeValueException;
}

class UnityImageValueComponent extends CustomComponent
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, UnityImageValueComponent.class);
	static final int PREVIEW_WIDTH = 256;
	static final int PREVIEW_HEIGHT = 128;
	
	private Image field;
	private Upload upload;
	private ProgressBar progressIndicator;
	private CheckBox scale;
	private Label error;
	private UnityImage value;
	private UnityMessageSource msg;
	private boolean required;
	
	UnityImageValueComponent(UnityImage initialValue,
			ImageConfiguration imgConfig,
			UnityMessageSource msg)
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
				LOG.warn("Problem getting value's image as resource for editing: " + e, e);
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
	
	UnityImage getValue(ImageValidator validator) throws IllegalAttributeValueException
	{
		if (value == null && !required)
			return null;
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
		return value;
	}

	static Label getErrorImage()
	{
		Label errorImage = new Label(Images.error.getHtml());
		errorImage.setContentMode(ContentMode.HTML);
		errorImage.addStyleName(Styles.largeIcon.toString());
		return errorImage;
	}

	static Component getHints(ImageConfiguration imgConfig, UnityMessageSource msg)
	{
		Label ret = new Label(msg.getMessage("ImageAttributeHandler.maxSize", imgConfig.getMaxSize() / 1024) + "  "
				+ msg.getMessage("ImageAttributeHandler.maxDim", imgConfig.getMaxWidth(), imgConfig.getMaxHeight()));
		ret.addStyleName(Styles.vLabelSmall.toString());
		return ret;
	}

	void setValueRequired(boolean required)
	{
		this.required = required;
	}
	
	/**
	 * Simple dialog showing image in it's default size.
	 */
	private class ShowImageDialog extends AbstractDialog
	{
		private UnityImage image;

		public ShowImageDialog(UnityImage image, UnityMessageSource msg)
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
