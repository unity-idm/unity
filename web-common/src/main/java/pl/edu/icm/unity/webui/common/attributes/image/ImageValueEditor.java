/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.webui.common.*;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Editor for Image values.
 *
 * @author R. Ledzinski
 */
class ImageValueEditor implements AttributeValueEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ImageValueEditor.class);
	private static final int PREVIEW_WIDTH = 256;
	private static final int PREVIEW_HEIGHT = 128;
	private UnityImage value;
	private String label;
	private Image field;
	private Upload upload;
	private ProgressBar progressIndicator;
	private CheckBox scale;
	private Label error;
	private boolean required;
	private UnityMessageSource msg;
	private ImageAttributeSyntax syntax;

	ImageValueEditor(String valueRaw, String label, UnityMessageSource msg, ImageAttributeSyntax syntax)
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
				field.setSource(new SimpleImageSource(
						value.getScaledDownImage(PREVIEW_WIDTH, PREVIEW_HEIGHT),
						value.getType())
						.getResource());
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
		private ImageAttributeSyntax syntax;
		private UnityImage.ImageType type;

		public ImageUploader(Image image, ImageAttributeSyntax syntax, ProgressBar progress)
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
				value = new UnityImage(
						((ByteArrayOutputStream) fos.getWrappedStream()).toByteArray(),
						type);

				if (scale.getValue())
					value.scaleDown(syntax.getMaxWidth(), syntax.getMaxHeight());

				image.setSource(new SimpleImageSource(
						value.getScaledDownImage(PREVIEW_WIDTH, PREVIEW_HEIGHT),
						value.getType()).
						getResource());
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

	static Component getHints(ImageAttributeSyntax syntax, UnityMessageSource msg)
	{
		Label ret = new Label(msg.getMessage("ImageAttributeHandler.maxSize", syntax.getMaxSize())
				+ "  " +
				msg.getMessage("ImageAttributeHandler.maxDim", syntax.getMaxWidth(),
						syntax.getMaxHeight()));
		ret.addStyleName(Styles.vLabelSmall.toString());
		return ret;
	}

	/**
	 * Simple dialog showing image in it's default size.
	 */
	private class ShowImageDialog extends AbstractDialog
	{
		private UnityImage image;

		public ShowImageDialog(UnityImage image)
		{
			super(ImageValueEditor.this.msg,
					ImageValueEditor.this.msg.getMessage("ImageAttributeHandler.image"),
					ImageValueEditor.this.msg.getMessage("close"));
			this.image = image;
			setSizeMode(SizeMode.LARGE);
		}

		@Override
		protected Component getContents() throws Exception
		{
			Image imageC = new Image();
			imageC.setSource(new SimpleImageSource(image.getImage(), image.getType()).getResource());
			return imageC;
		}

		@Override
		protected void onConfirm()
		{
			close();
		}
	}
}
