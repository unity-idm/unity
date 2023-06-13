/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import static pl.edu.icm.unity.webui.common.attributes.image.UnityImageValueComponent.PREVIEW_HEIGHT;
import static pl.edu.icm.unity.webui.common.attributes.image.UnityImageValueComponent.PREVIEW_WIDTH;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Image;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;

import pl.edu.icm.unity.base.attr.ImageType;
import pl.edu.icm.unity.base.attr.UnityImage;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;
import pl.edu.icm.unity.webui.common.AbstractUploadReceiver;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.NotificationPopup;

class UnityImageUploader extends AbstractUploadReceiver
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, UnityImageUploader.class);
	
	private final ImageConfiguration imgConfig;
	private final MessageSource msg;
	private final CheckBox scale;
	private final Consumer<UnityImage> uploadedImageConsumer;
	
	private Image image;
	private LimitedOuputStream fos;
	private ImageType type;
	private UnityImage uploadedImage;

	UnityImageUploader(Image image,
			Upload upload,
			ImageConfiguration imgConfig,
			MessageSource msg,
			ProgressBar progress,
			CheckBox scale,
			Consumer<UnityImage> uploadedImageConsumer)
	{
		super(upload, progress);
		this.image = image;
		this.imgConfig = imgConfig;
		this.msg = msg;
		this.scale = scale;
		this.uploadedImageConsumer = uploadedImageConsumer;
	}

	@Override
	public OutputStream receiveUpload(String filename, String mimeType)
	{
		int length = imgConfig.getMaxSize();
		fos = new LimitedOuputStream(length, new ByteArrayOutputStream(length > 102400 ? 102400 : length));
		return fos;
	}

	@Override
	public void uploadStarted(Upload.StartedEvent event)
	{
		try
		{
			type = ImageType.fromMimeType(event.getMIMEType());
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
			uploadedImage = new UnityImage(((ByteArrayOutputStream) fos.getWrappedStream()).toByteArray(), type);
			if (scale.getValue())
				uploadedImage.scaleDown(imgConfig.getMaxWidth(), imgConfig.getMaxHeight());
			
			byte[] scaledDownData = uploadedImage.getScaledDownImage(PREVIEW_WIDTH, PREVIEW_HEIGHT);
			UnityImage scaledDown = new UnityImage(scaledDownData, uploadedImage.getType());
			image.setSource(new SimpleImageSource(scaledDown).getResource());
			
			uploadedImageConsumer.accept(uploadedImage);
		} catch (Exception e)
		{
			LOG.warn("Unable to upload file.", e);
			NotificationPopup.showError(msg.getMessage("ImageAttributeHandler.uploadInvalid"), "");
			fos = null;
		}
	}
}

