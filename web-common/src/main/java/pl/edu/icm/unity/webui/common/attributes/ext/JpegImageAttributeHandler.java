/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.LimitedByteArrayOuputStream;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;

import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.ProgressListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

/**
 * Jpeg image attribute handler for the web
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class JpegImageAttributeHandler implements WebAttributeHandler<BufferedImage>, WebAttributeHandlerFactory
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, JpegImageAttributeHandler.class);
	private static final Random r = new Random();
	private UnityMessageSource msg;
	
	@Autowired
	public JpegImageAttributeHandler(UnityMessageSource msg)
	{
		this.msg = msg;
	}
	
	@Override
	public String getSupportedSyntaxId()
	{
		return JpegImageAttributeSyntax.ID;
	}

	@Override
	public String getValueAsString(BufferedImage value,
			AttributeValueSyntax<BufferedImage> syntax, int limited)
	{
		return "Jpeg image";
	}

	@Override
	public Resource getValueAsImage(BufferedImage value,
			AttributeValueSyntax<BufferedImage> syntax, int maxWidth, int maxHeight)
	{
		try
		{
			BufferedImage scaled = scaleIfNeeded(value, maxWidth, maxHeight);
			SimpleImageSource source = new SimpleImageSource(scaled, syntax, "jpg");
			return source.getResource();
		} catch (Exception e)
		{
			log.warn("Problem getting value's image as resource: " + e, e);
			return Images.error32.getResource();
		}
	}

	@Override
	public Component getRepresentation(BufferedImage value,
			AttributeValueSyntax<BufferedImage> syntax)
	{
		Panel ret = new Panel();
		Image image = new Image();
		image.setSource(getValueAsImage(value, syntax, value.getWidth(), value.getHeight()));
		ret.setContent(image);
		return ret;
	}

	private BufferedImage scaleIfNeeded(BufferedImage value, int maxWidth, int maxHeight)
	{
		int w = value.getWidth();
		int h = value.getHeight();
		if (w<=maxWidth && h<=maxHeight)
			return value;
		
		double ratioW = maxWidth/(double)w;
		double ratioH = maxHeight/(double)h;
		double ratio = ratioW > ratioH ? ratioH : ratioW;
		int newWidth = new Double(w * ratio).intValue();
		int newHeight = new Double(h * ratio).intValue();
		
		BufferedImage resized = new BufferedImage(newWidth, newHeight, value.getType());
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(value, 0, 0, newWidth, newHeight, 0, 0, w, h, null);
		g.dispose();
		return resized;
	}
	
	@Override
	public AttributeValueEditor<BufferedImage> getEditorComponent(BufferedImage initialValue, 
			AttributeValueSyntax<BufferedImage> syntax)
	{
		return new JpegImageValueEditor(initialValue, (JpegImageAttributeSyntax) syntax);
	}
	
	private class JpegImageValueEditor implements AttributeValueEditor<BufferedImage>
	{
		private BufferedImage value;
		private JpegImageAttributeSyntax syntax;
		private Image field;
		private Upload upload;
		private ProgressIndicator progressIndicator;
		private CheckBox scale;
		private Label error;
		
		public JpegImageValueEditor(BufferedImage value, JpegImageAttributeSyntax syntax)
		{
			this.value = value;
			this.syntax = syntax;
		}

		@Override
		public Component getEditor()
		{
			VerticalLayout vl = new VerticalLayout();
			vl.setSpacing(true);
			
			error = new Label();
			vl.addComponent(error);
			error.setStyleName(Styles.error.toString());
			
			field = new Image();
			if (value != null)
			{
				field = new Image();
				try
				{
					SimpleImageSource source = new SimpleImageSource(value, syntax, "jpg");
					field.setSource(source.getResource());
				} catch (Exception e)
				{
					log.warn("Problem getting value's image as resource for editing: " + e, e);
					field.setSource(Images.error32.getResource());
				}
			}
			vl.addComponent(field);
			
			upload = new Upload();
			ImageUploader uploader = new ImageUploader(field, syntax); 
			upload.setReceiver(uploader);
			upload.addSucceededListener(uploader);
			upload.addStartedListener(uploader);
			upload.addProgressListener(uploader);
			upload.setWidth(100, Unit.PERCENTAGE);
			vl.addComponent(upload);
			progressIndicator = new ProgressIndicator(0);
			progressIndicator.setVisible(false);
			progressIndicator.setPollingInterval(2000);
			vl.addComponent(progressIndicator);
			scale = new CheckBox(msg.getMessage("JpegAttributeHandler.scaleIfNeeded"));
			scale.setValue(true);
			vl.addComponent(scale);
			setHints(vl, syntax);
			return vl;
		}

		@Override
		public BufferedImage getCurrentValue() throws IllegalAttributeValueException
		{
			if (value == null)
			{
				error.setValue(msg.getMessage("JpegAttributeHandler.noImage"));
				throw new IllegalAttributeValueException(msg.getMessage("JpegAttributeHandler.noImage"));
			}
			try
			{
				syntax.validate(value);
			} catch (IllegalAttributeValueException e)
			{
				error.setValue(e.getMessage());
				field.setVisible(false);
				throw e;
			}
			
			error.setValue("");
			return value;
		}
		
		private class ImageUploader implements Receiver, SucceededListener, StartedListener,
			ProgressListener
		{
			private Image image;
			private LimitedByteArrayOuputStream fos;
			private JpegImageAttributeSyntax syntax;
			
			public ImageUploader(Image image, JpegImageAttributeSyntax syntax)
			{
				this.image = image;
				this.syntax = syntax;
			}

			public OutputStream receiveUpload(String filename, String mimeType) 
			{
				fos = new LimitedByteArrayOuputStream(syntax.getMaxSize());
				return fos;
			}

			public void uploadSucceeded(SucceededEvent event) 
			{
				progressIndicator.setVisible(false);
				upload.setEnabled(true);
				
				if (fos.isOverflow())
				{
					ErrorPopup.showError(msg.getMessage("JpegAttributeHandler.uploadFailed"),
							msg.getMessage("JpegAttributeHandler.imageSizeTooBig"));
					fos = null;
					return;
				}
				try
				{
					image.setVisible(true);
					value = syntax.deserialize(fos.toByteArray());
					if (scale.getValue())
						value = scaleIfNeeded(value, syntax.getMaxWidth(), syntax.getMaxHeight());
					image.setSource(new SimpleImageSource(value, syntax, "jpg").getResource());
				} catch (Exception e)
				{
					ErrorPopup.showError(msg.getMessage("JpegAttributeHandler.uploadInvalid"),
							"");
					fos = null;
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void uploadStarted(StartedEvent event)
			{
				upload.setEnabled(false);
				image.setVisible(false);
				long length = event.getContentLength();
				if (length <= 0)
					progressIndicator.setIndeterminate(true);
				else
					progressIndicator.setIndeterminate(false);
				progressIndicator.setVisible(true);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void updateProgress(long readBytes, long contentLength)
			{
				if (contentLength > 0 && !progressIndicator.isIndeterminate())
				{
					progressIndicator.setValue((float)readBytes/contentLength);
				}
			}
		};		
	}
	
	private void setHints(VerticalLayout vl, JpegImageAttributeSyntax syntax)
	{
		Label maxSize = new Label(msg.getMessage("JpegAttributeHandler.maxSize", syntax.getMaxSize()));
		vl.addComponent(maxSize);
		Label maxDim = new Label(msg.getMessage("JpegAttributeHandler.maxDim", syntax.getMaxWidth(),
				syntax.getMaxHeight()));
		vl.addComponent(maxDim);
	}
	
	public class SimpleImageSource implements StreamSource
	{
		private static final long serialVersionUID = 1L;
		private final byte[] isData;
		private final String extension;
		
		public SimpleImageSource(BufferedImage value, 
				AttributeValueSyntax<BufferedImage> syntax, String extension)
		{
			this.isData = syntax.serialize(value);
			this.extension = extension;
		}
		
		@Override
		public InputStream getStream()
		{
			return new ByteArrayInputStream(isData);
		}
		
		public Resource getResource()
		{
			return new StreamResource(this, "imgattribute-"+r.nextLong()+r.nextLong()
					+"." + extension);
		}
	}
	
	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new JpegImageAttributeHandler(msg);
	}

	@Override
	public Component getSyntaxViewer(AttributeValueSyntax<BufferedImage> syntax)
	{
		VerticalLayout vl = new VerticalLayout();
		setHints(vl, (JpegImageAttributeSyntax) syntax);
		return vl;
	}

	@Override
	public AttributeSyntaxEditor<BufferedImage> getSyntaxEditorComponent(
			AttributeValueSyntax<BufferedImage> initialValue)
	{
		return new JpegSyntaxEditor((JpegImageAttributeSyntax) initialValue);
	}
	
	private class JpegSyntaxEditor implements AttributeSyntaxEditor<BufferedImage>
	{
		private JpegImageAttributeSyntax initial;
		private IntegerBoundEditor maxHeight, maxWidth, maxSize;
		
		
		public JpegSyntaxEditor(JpegImageAttributeSyntax initial)
		{
			this.initial = initial;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new FormLayout();
			maxWidth = new IntegerBoundEditor(msg, msg.getMessage("JpegAttributeHandler.maxWidthUnlimited"), 
					msg.getMessage("JpegAttributeHandler.maxWidthE"), Integer.MAX_VALUE);
			maxWidth.setMin(1);
			maxHeight = new IntegerBoundEditor(msg, msg.getMessage("JpegAttributeHandler.maxHeightUnlimited"), 
					msg.getMessage("JpegAttributeHandler.maxHeightE"), Integer.MAX_VALUE);
			maxHeight.setMin(1);
			maxSize = new IntegerBoundEditor(msg, msg.getMessage("JpegAttributeHandler.maxSizeUnlimited"), 
					msg.getMessage("JpegAttributeHandler.maxSizeE"), Integer.MAX_VALUE);
			maxSize.setMin(100);
			fl.addComponents(maxWidth, maxHeight, maxSize);
			if (initial != null)
			{
				maxWidth.setValue(initial.getMaxWidth());
				maxHeight.setValue(initial.getMaxHeight());
				maxSize.setValue(initial.getMaxSize());
			} else
			{
				maxWidth.setValue(200);
				maxHeight.setValue(200);
				maxSize.setValue(1024000);
			}
			return fl;
		}

		@Override
		public AttributeValueSyntax<BufferedImage> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			try
			{
				JpegImageAttributeSyntax ret = new JpegImageAttributeSyntax();
				ret.setMaxHeight((int)(long)maxHeight.getValue());
				ret.setMaxWidth((int)(long)maxWidth.getValue());
				ret.setMaxSize((int)(long)maxSize.getValue());
				return ret;
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException(e.getMessage(), e);
			}
		}
		
	}
}
