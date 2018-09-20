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

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Binder;
import com.vaadin.event.MouseEvents;
import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.SucceededEvent;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.AbstractUploadReceiver;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;

/**
 * Jpeg image attribute handler for the web
 * @author K. Benedyczak
 */
public class JpegImageAttributeHandler implements WebAttributeHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, JpegImageAttributeHandler.class);
	private static final Random r = new Random();
	private static final int PREVIEW_WIDTH = 256;
	private static final int PREVIEW_HEIGHT = 128;
	private UnityMessageSource msg;
	private JpegImageAttributeSyntax syntax;
	
	public JpegImageAttributeHandler(UnityMessageSource msg, AttributeValueSyntax<?> syntax)
	{
		this.msg = msg;
		this.syntax = (JpegImageAttributeSyntax) syntax;
	}

	@Override
	public String getValueAsString(String value)
	{
		return "Jpeg image";
	}

	private Resource getValueAsImage(BufferedImage value,
			JpegImageAttributeSyntax syntax, int maxWidth, int maxHeight)
	{
		try
		{
			BufferedImage scaled = scaleIfNeeded(value, maxWidth, maxHeight);
			SimpleImageSource source = new SimpleImageSource(scaled, syntax, "jpg");
			return source.getResource();
		} catch (Exception e)
		{
			log.warn("Problem getting value's image as resource: " + e, e);
			return null;
		}
	}

	@Override
	public Component getRepresentation(String valueRaw, AttributeViewerContext context)
	{
		BufferedImage value = syntax.convertFromString(valueRaw);
		if (value == null)
			return  getErrorImage();
		
		int width = value.getWidth();
		int height = value.getHeight();
		Resource resValue = getValueAsImage(value, syntax, width,
				height);
		if (resValue != null)
		{
			Image image = new Image();

			image.setSource(resValue);
			return image;
		} else
		{
			return  getErrorImage();
		}	
	}
	
	private Label getErrorImage()
	{
		Label errorImage =  new Label(Images.error.getHtml());
		errorImage.setContentMode(ContentMode.HTML);
		errorImage.addStyleName(Styles.largeIcon.toString());
		return errorImage;
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
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new JpegImageValueEditor(initialValue, label);
	}
	
	private class JpegImageValueEditor implements AttributeValueEditor
	{
		private BufferedImage value;
		private String label;
		private Image field;
		private Upload upload;
		private ProgressBar progressIndicator;
		private CheckBox scale;
		private Label error;
		private boolean required;
		
		public JpegImageValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
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
					BufferedImage scalledPreview = scaleIfNeeded(value, PREVIEW_WIDTH, PREVIEW_HEIGHT);
					SimpleImageSource source = new SimpleImageSource(scalledPreview, syntax, "jpg");
					field.setSource(source.getResource());
					errorImage.setVisible(false);
					field.setVisible(true);
				} catch (Exception e)
				{
					log.warn("Problem getting value's image as resource for editing: " + e, e);
					errorImage.setVisible(true);
					field.setVisible(false);
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
			upload.setCaption(label);

			scale = new CheckBox(msg.getMessage("JpegAttributeHandler.scaleIfNeeded"));
			scale.setValue(true);
			return new ComponentsContainer(field, errorImage, error, upload, progressIndicator, scale, 
					getHints(syntax));
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
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
			return syntax.convertToString(value);
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
					NotificationPopup.showError(
							msg.getMessage("JpegAttributeHandler.uploadFailed"),
							msg.getMessage("JpegAttributeHandler.imageSizeTooBig"));
					fos = null;
					return;
				}
				try
				{
					image.setVisible(true);
					value = syntax.deserialize(((ByteArrayOutputStream)
							fos.getWrappedStream()).toByteArray());
					if (scale.getValue())
						value = scaleIfNeeded(value, syntax.getMaxWidth(), 
								syntax.getMaxHeight());
					BufferedImage scalledPreview = scaleIfNeeded(value, 
							PREVIEW_WIDTH, PREVIEW_HEIGHT);
					image.setSource(new SimpleImageSource(scalledPreview, syntax, "jpg").
							getResource());
				} catch (Exception e)
				{
					NotificationPopup.showError(msg.getMessage("JpegAttributeHandler.uploadInvalid"),
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
		Label ret = new Label(msg.getMessage("JpegAttributeHandler.maxSize", syntax.getMaxSize())
				+ "  " + 
				msg.getMessage("JpegAttributeHandler.maxDim", syntax.getMaxWidth(), 
						syntax.getMaxHeight()));
		ret.addStyleName(Styles.vLabelSmall.toString());
		return ret;
	}
	
	public static class SimpleImageSource implements StreamSource
	{
		private final byte[] isData;
		private final String extension;
		
		public SimpleImageSource(BufferedImage value, 
				JpegImageAttributeSyntax syntax, String extension)
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
	public Component getSyntaxViewer()
	{
		return new CompactFormLayout(getHints(syntax));
	}

	
	private static class JpegSyntaxEditor implements AttributeSyntaxEditor<BufferedImage>
	{
		private JpegImageAttributeSyntax initial;
		private IntegerBoundEditor maxHeight, maxSize;
		private IntegerBoundEditor maxWidth;
		private UnityMessageSource msg;
		private Binder<JpegSyntaxBindingValue> binder;

		public JpegSyntaxEditor(JpegImageAttributeSyntax initial, UnityMessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new CompactFormLayout();
			maxWidth = new IntegerBoundEditor(msg,
					msg.getMessage("JpegAttributeHandler.maxWidthUnlimited"),
					msg.getMessage("JpegAttributeHandler.maxWidthE"),
					Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
			maxHeight = new IntegerBoundEditor(msg,
					msg.getMessage("JpegAttributeHandler.maxHeightUnlimited"),
					msg.getMessage("JpegAttributeHandler.maxHeightE"),
					Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
			maxSize = new IntegerBoundEditor(msg,
					msg.getMessage("JpegAttributeHandler.maxSizeUnlimited"),
					msg.getMessage("JpegAttributeHandler.maxSizeE"),
					Integer.MAX_VALUE, 100, Integer.MAX_VALUE);

			binder = new Binder<>(JpegSyntaxBindingValue.class);
			maxWidth.configureBinding(binder, "maxWidth");
			maxHeight.configureBinding(binder, "maxHeight");
			maxSize.configureBinding(binder, "maxSize");

			fl.addComponents(maxWidth, maxHeight, maxSize);

			JpegSyntaxBindingValue value = new JpegSyntaxBindingValue();
			if (initial != null)
			{
				value.setMaxWidth(initial.getMaxWidth());
				value.setMaxHeight(initial.getMaxHeight());
				value.setMaxSize(initial.getMaxSize());
			} else
			{
				value.setMaxWidth(200);
				value.setMaxHeight(200);
				value.setMaxSize(1024000);
			}
			binder.setBean(value);
			return fl;
		}

		@Override
		public AttributeValueSyntax<BufferedImage> getCurrentValue()
				throws IllegalAttributeTypeException
		{

			try
			{
				if (!binder.isValid())
				{
					binder.validate();
					throw new IllegalAttributeTypeException("");
				}

				JpegSyntaxBindingValue value = binder.getBean();
				JpegImageAttributeSyntax ret = new JpegImageAttributeSyntax();
				ret.setMaxHeight(value.getMaxHeight());
				ret.setMaxWidth(value.getMaxWidth());
				ret.setMaxSize(value.getMaxSize());
				return ret;
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException(e.getMessage(), e);
			}

		}

		public class JpegSyntaxBindingValue
		{
			private Integer maxSize;
			private Integer maxWidth;
			private Integer maxHeight;
			
			public JpegSyntaxBindingValue()
			{

			}

			public Integer getMaxSize()
			{
				return maxSize;
			}

			public void setMaxSize(Integer maxSize)
			{
				this.maxSize = maxSize;
			}

			public Integer getMaxWidth()
			{
				return maxWidth;
			}

			public void setMaxWidth(Integer maxWidth)
			{
				this.maxWidth = maxWidth;
			}

			public Integer getMaxHeight()
			{
				return maxHeight;
			}

			public void setMaxHeight(Integer maxHeight)
			{
				this.maxHeight = maxHeight;
			}
		}
	}
	
	private class ShowImageDialog extends AbstractDialog
	{
		private JpegImageAttributeSyntax syntax;
		private BufferedImage image;
		
		public ShowImageDialog(JpegImageAttributeSyntax syntax, BufferedImage image)
		{
			super(JpegImageAttributeHandler.this.msg, 
					JpegImageAttributeHandler.this.msg.getMessage("JpegAttributeHandler.image"), 
					JpegImageAttributeHandler.this.msg.getMessage("close"));
			this.syntax = syntax;
			this.image = image;
			setSizeMode(SizeMode.LARGE);
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
	
	
	
	@org.springframework.stereotype.Component
	public static class JpegImageAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;

		@Autowired
		public JpegImageAttributeHandlerFactory(UnityMessageSource msg)
		{
			this.msg = msg;
		}
		
		
		@Override
		public String getSupportedSyntaxId()
		{
			return JpegImageAttributeSyntax.ID;
		}
		
		@Override
		public AttributeSyntaxEditor<BufferedImage> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new JpegSyntaxEditor((JpegImageAttributeSyntax) initialValue, msg);
		}
		
		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new JpegImageAttributeHandler(msg, syntax);
		}
	}
}
