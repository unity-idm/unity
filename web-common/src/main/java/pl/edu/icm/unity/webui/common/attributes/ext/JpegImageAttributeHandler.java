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

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
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
			SimpleImageSource source = new SimpleImageSource(scaled, syntax);
			return source.getResource();
		} catch (Exception e)
		{
			log.warn("Problem getting value's image as resource: " + e, e);
			return Images.unknown.getResource();
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
			//FIXME - rather not working. 
			error.setStyleName(Styles.error.toString());
			
			field = new Image();
			if (value != null)
			{
				field = new Image();
				try
				{
					SimpleImageSource source = new SimpleImageSource(value, syntax);
					field.setSource(source.getResource());
				} catch (Exception e)
				{
					log.warn("Problem getting value's image as resource for editing: " + e, e);
					field.setSource(Images.unknown.getResource());
				}
			}
			vl.addComponent(field);
			
			upload = new Upload();
			ImageUploader uploader = new ImageUploader(field, syntax); 
			upload.setReceiver(uploader);
			upload.addSucceededListener(uploader);
			vl.addComponent(upload);
			
			Label maxSize = new Label(msg.getMessage("JpegAttributeHandler.maxSize", syntax.getMaxSize()));
			vl.addComponent(maxSize);
			Label maxDim = new Label(msg.getMessage("JpegAttributeHandler.maxDim", syntax.getMaxWidth(),
					syntax.getMaxHeight()));
			vl.addComponent(maxDim);
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
				throw e;
			}
			
			error.setValue("");
			return value;
		}
		
		private class ImageUploader implements Receiver, SucceededListener {
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
					image.setSource(new SimpleImageSource(value, syntax).getResource());
				} catch (Exception e)
				{
					ErrorPopup.showError(msg.getMessage("JpegAttributeHandler.uploadInvalid"),
							"");
					fos = null;
				}
			}
		};		
	}
	
	public class SimpleImageSource implements StreamSource
	{
		private static final long serialVersionUID = 1L;
		private InputStream is;
		
		public SimpleImageSource(BufferedImage value, 
				AttributeValueSyntax<BufferedImage> syntax)
		{
			this.is = new ByteArrayInputStream(syntax.serialize(value));
		}
		
		public InputStream getStream()
		{
			return is;
		}
		
		public Resource getResource()
		{
			return new StreamResource(this, "imgattribute-"+r.nextLong()+r.nextLong());
		}
	}
	
	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new JpegImageAttributeHandler(msg);
	}
}
