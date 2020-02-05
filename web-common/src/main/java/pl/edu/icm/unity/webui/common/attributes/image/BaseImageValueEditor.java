/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.BaseImageAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.stdext.utils.UnityImageSpec;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.AbstractUploadReceiver;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

/**
 * Editor for Image values.
 *
 * @author R. Ledzinski
 */
class BaseImageValueEditor<T extends UnityImageSpec> implements AttributeValueEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, BaseImageValueEditor.class);
	private static final int PREVIEW_WIDTH = 256;
	private static final int PREVIEW_HEIGHT = 128;
	private T value;
	private String label;
	private Image field;
	private Upload upload;
	private ProgressBar progressIndicator;
	private CheckBox scale;
	private Label error;
	private boolean required;
	private UnityMessageSource msg;
	private BaseImageAttributeSyntax<T> syntax;

	BaseImageValueEditor(String valueRaw, String label, UnityMessageSource msg, BaseImageAttributeSyntax<T> syntax)
	{
		this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
		this.label = label;
		this.msg = msg;
		this.syntax = syntax;
	}

	@Override
	public ComponentsContainer getEditor(AttributeEditContext context)
	{
		error = new Label();
		error.setStyleName(Styles.error.toString());
		error.setVisible(false);

		required = context.isRequired();

		Label errorImage = getErrorImage();
		errorImage.setVisible(false);

		field = new Image();
		if (value != null)
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
				log.warn("Problem getting value's image as resource for editing: " + e, e);
				errorImage.setVisible(true);
				field.setVisible(false);
			}
		}

		field.addClickListener(event ->
				{
					if (value != null)
						new ShowImageDialog(value).show();
				}
		);

		field.setDescription(msg.getMessage("ImageAttributeHandler.clickToEnlarge"));

		upload = new Upload();
		progressIndicator = new ProgressBar(0);
		progressIndicator.setVisible(false);

		ImageUploader uploader = new ImageUploader(field, syntax, progressIndicator);
		uploader.register();
		upload.setCaption(label);

		upload.setAcceptMimeTypes(
				UnityImage.ImageType.getSupportedMimeTypes(","));

		scale = new CheckBox(msg.getMessage("ImageAttributeHandler.scaleIfNeeded"));
		scale.setValue(true);
		return new ComponentsContainer(field, errorImage, error, upload, progressIndicator, scale,
				getHints(syntax, msg));
	}

	@Override
	public String getCurrentValue() throws IllegalAttributeValueException
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
			syntax.validate(value);
		} catch (IllegalAttributeValueException e)
		{
			error.setValue(e.getMessage());
			error.setVisible(true);
			field.setVisible(false);
			throw e;
		}

		error.setVisible(false);
		field.setVisible(true);
		return syntax.convertToString(value);
	}

	private class ImageUploader extends AbstractUploadReceiver
	{
		private Image image;
		private LimitedOuputStream fos;
		private BaseImageAttributeSyntax<T> syntax;
		private UnityImage.ImageType type;

		public ImageUploader(Image image, BaseImageAttributeSyntax<T> syntax, ProgressBar progress)
		{
			super(upload, progress);
			this.image = image;
			this.syntax = syntax;
		}

		@Override
		public OutputStream receiveUpload(String filename, String mimeType)
		{
			int length = syntax.getMaxSize();
			fos = new LimitedOuputStream(length,
					new ByteArrayOutputStream(length > 102400 ? 102400 : length));
			return fos;
		}

		@Override
		public void uploadStarted(Upload.StartedEvent event)
		{
			try
			{
				type = UnityImage.ImageType.fromMimeType(event.getMIMEType());
			} catch (RuntimeException e)
			{
				NotificationPopup.showError(
						msg.getMessage("ImageAttributeHandler.uploadFailed"),
						msg.getMessage("ImageAttributeHandler.formatNotSupported"));
				upload.interruptUpload();
				return;
			}

			super.uploadStarted(event);
		}

		@Override
		public void uploadSucceeded(Upload.SucceededEvent event)
		{
			super.uploadSucceeded(event);

			if (fos.isOverflow())
			{
				NotificationPopup.showError(
						msg.getMessage("ImageAttributeHandler.uploadFailed"),
						msg.getMessage("ImageAttributeHandler.imageSizeTooBig"));
				fos = null;
				return;
			}
			try
			{
				image.setVisible(true);
				value = syntax.newImage(value, ((ByteArrayOutputStream) fos.getWrappedStream()).toByteArray(), type);

				if (scale.getValue())
					value.scaleDown(syntax.getConfig().getMaxWidth(), syntax.getConfig().getMaxHeight());

				byte[] scaledDownData = value.getScaledDownImage(PREVIEW_WIDTH, PREVIEW_HEIGHT);
				UnityImage scaledDown = new UnityImage(scaledDownData, value.getType());
				image.setSource(new SimpleImageSource(scaledDown).getResource());
			} catch (Exception e)
			{
				NotificationPopup.showError(msg.getMessage("ImageAttributeHandler.uploadInvalid"),
						"");
				fos = null;
			}
		}
	}

	@Override
	public void setLabel(String label)
	{
		upload.setCaption(label);
	}

	static Label getErrorImage()
	{
		Label errorImage = new Label(Images.error.getHtml());
		errorImage.setContentMode(ContentMode.HTML);
		errorImage.addStyleName(Styles.largeIcon.toString());
		return errorImage;
	}

	static Component getHints(BaseImageAttributeSyntax<?> syntax, UnityMessageSource msg)
	{
		Label ret = new Label(msg.getMessage("ImageAttributeHandler.maxSize", syntax.getMaxSize()/1024)
				+ "  " +
				msg.getMessage("ImageAttributeHandler.maxDim", syntax.getConfig().getMaxWidth(),
						syntax.getConfig().getMaxHeight()));
		ret.addStyleName(Styles.vLabelSmall.toString());
		return ret;
	}

	/**
	 * Simple dialog showing image in it's default size.
	 */
	private class ShowImageDialog extends AbstractDialog
	{
		private UnityImageSpec image;

		public ShowImageDialog(UnityImageSpec image)
		{
			super(BaseImageValueEditor.this.msg,
					BaseImageValueEditor.this.msg.getMessage("ImageAttributeHandler.image"),
					BaseImageValueEditor.this.msg.getMessage("close"));
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
