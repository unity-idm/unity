/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.AbstractUploadReceiver;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.SafePanel;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;

import com.vaadin.event.MouseEvents;
import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.SucceededEvent;

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
	private static final int PREVIEW_WIDTH = 256;
	private static final int PREVIEW_HEIGHT = 128;
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
		Panel ret = new SafePanel();
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
	public AttributeValueEditor<BufferedImage> getEditorComponent(BufferedImage initialValue, String label,
			AttributeValueSyntax<BufferedImage> syntax)
	{
		return new JpegImageValueEditor(initialValue, label, (JpegImageAttributeSyntax) syntax);
	}
	
	private class JpegImageValueEditor implements AttributeValueEditor<BufferedImage>
	{
		private BufferedImage value;
		private String label;
		private JpegImageAttributeSyntax syntax;
		private Image field;
		private Upload upload;
		private ProgressBar progressIndicator;
		private CheckBox scale;
		private Label error;
		private boolean required;
		
		public JpegImageValueEditor(BufferedImage value, String label, JpegImageAttributeSyntax syntax)
		{
			this.value = value;
			this.syntax = syntax;
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(boolean required)
		{
			error = new Label();
			error.setStyleName(Styles.error.toString());
			error.setVisible(false);
			
			field = new Image();
			if (value != null)
			{
				try
				{
					BufferedImage scalledPreview = scaleIfNeeded(value, PREVIEW_WIDTH, PREVIEW_HEIGHT);
					SimpleImageSource source = new SimpleImageSource(scalledPreview, syntax, "jpg");
					field.setSource(source.getResource());
				} catch (Exception e)
				{
					log.warn("Problem getting value's image as resource for editing: " + e, e);
					field.setSource(Images.error32.getResource());
				}
			}
			field.addClickListener(new MouseEvents.ClickListener()
			{
				@Override
				public void click(ClickEvent event)
				{
					if (value != null)
						new ShowImageDialog(syntax, value).show();
				}
			});
			field.setDescription(msg.getMessage("JpegAttributeHandler.clickToEnlarge"));
			
			upload = new Upload();
			progressIndicator = new ProgressBar(0);
			progressIndicator.setVisible(false);
			
			ImageUploader uploader = new ImageUploader(field, syntax, progressIndicator);
			uploader.register();
			upload.setWidth(100, Unit.PERCENTAGE);
			upload.setCaption(label);

			scale = new CheckBox(msg.getMessage("JpegAttributeHandler.scaleIfNeeded"));
			scale.setValue(true);
			return new ComponentsContainer(field, error, upload, progressIndicator, scale, 
					getHints(syntax));
		}

		@Override
		public BufferedImage getCurrentValue() throws IllegalAttributeValueException
		{
			if (value == null && !required)
				return null;
			if (value == null)
			{
				error.setValue(msg.getMessage("JpegAttributeHandler.noImage"));
				error.setVisible(true);
				field.setVisible(false);
				throw new IllegalAttributeValueException(msg.getMessage("JpegAttributeHandler.noImage"));
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
			return value;
		}
		
		private class ImageUploader extends AbstractUploadReceiver
		{
			private Image image;
			private LimitedOuputStream fos;
			private JpegImageAttributeSyntax syntax;
			
			public ImageUploader(Image image, JpegImageAttributeSyntax syntax, ProgressBar progress)
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
			public void uploadSucceeded(SucceededEvent event) 
			{
				super.uploadSucceeded(event);
				
				if (fos.isOverflow())
				{
					ErrorPopup.showError(msg, msg.getMessage("JpegAttributeHandler.uploadFailed"),
							msg.getMessage("JpegAttributeHandler.imageSizeTooBig"));
					fos = null;
					return;
				}
				try
				{
					image.setVisible(true);
					value = syntax.deserialize(((ByteArrayOutputStream)fos.getWrappedStream()).toByteArray());
					if (scale.getValue())
						value = scaleIfNeeded(value, syntax.getMaxWidth(), syntax.getMaxHeight());
					BufferedImage scalledPreview = scaleIfNeeded(value, PREVIEW_WIDTH, PREVIEW_HEIGHT);
					image.setSource(new SimpleImageSource(scalledPreview, syntax, "jpg").getResource());
				} catch (Exception e)
				{
					ErrorPopup.showError(msg, msg.getMessage("JpegAttributeHandler.uploadInvalid"),
							"");
					fos = null;
				}
			}
		}

		@Override
		public void setLabel(String label)
		{
			upload.setCaption(label);
		};		
	}
	
	private Component getHints(JpegImageAttributeSyntax syntax)
	{
		return new Label(msg.getMessage("JpegAttributeHandler.maxSize", syntax.getMaxSize())
				+ "  " + 
				msg.getMessage("JpegAttributeHandler.maxDim", syntax.getMaxWidth(), 
						syntax.getMaxHeight()));
	}
	
	public static class SimpleImageSource implements StreamSource
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
		return new FormLayout(getHints((JpegImageAttributeSyntax)syntax));
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
	
	private class ShowImageDialog extends AbstractDialog
	{
		private AttributeValueSyntax<BufferedImage> syntax;
		private BufferedImage image;
		
		public ShowImageDialog(AttributeValueSyntax<BufferedImage> syntax, BufferedImage image)
		{
			super(JpegImageAttributeHandler.this.msg, 
					JpegImageAttributeHandler.this.msg.getMessage("JpegAttributeHandler.image"), 
					JpegImageAttributeHandler.this.msg.getMessage("close"));
			this.syntax = syntax;
			this.image = image;
			setWidth(90, Unit.PERCENTAGE);
			setHeight(90, Unit.PERCENTAGE);
		}

		@Override
		protected Component getContents() throws Exception
		{
			Image imageC = new Image();
			SimpleImageSource source = new SimpleImageSource(image, syntax, "jpg");
			imageC.setSource(source.getResource());
			return imageC;
		}

		@Override
		protected void onConfirm()
		{
			close();
		}
	}
}
