/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.imageio.ImageIO;

import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

import nl.captcha.Captcha;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.text.producer.DefaultTextProducer;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.webui.authn.credreset.TextFieldWithContextLabel;

/**
 * Vaadin captcha component. Allows for changing the captcha. Can be added as a standalone component,
 * or inserted to an existing {@link FormLayout}. In the latter case, the answer textfield will
 * become a regular form field.
 * @author K. Benedyczak
 */
public class CaptchaComponent 
{
	private static final char[] CAPTCHA_CHARS = new char[] { 'q', 'w', 'e', 'r',
		't', 'y', 'u', 'i', 'p', 'a', 's', 'd', 'f', 'g', 'h',
		'j', 'k', 'l', 'z', 'x', 'c', 'v', 'b', 'n', 'm',
		'1', '2', '3', '4', '5', '6', '7', '8', '9'};
	private static final Random random = new Random();
	private UnityMessageSource msg;
	private Captcha engine;
	
	private Image challenge;
	private TextFieldWithContextLabel answer;
	private Button resetChallenge;

	private int length;
	private boolean showLabelInline;
	
	public CaptchaComponent(UnityMessageSource msg, boolean showLabelInline)
	{
		this(msg, 6, showLabelInline);
	}

	public CaptchaComponent(UnityMessageSource msg, int length, boolean showLabelInline)
	{
		this.msg = msg;
		this.length = length;
		this.showLabelInline = showLabelInline;
		initEngine();
		initUI();
	}
	
	
	private void initEngine()
	{
		engine = new Captcha.Builder(31*length, 50)
			.addText(new DefaultTextProducer(length, CAPTCHA_CHARS))
			.addBackground(new GradiatedBackgroundProducer(new Color(0xf5, 0x92, 0x01), 
					new Color(0xE0, 0xE0, 0xE0)))
			.gimp(new FishEyeGimpyRenderer(Color.gray, Color.gray))
			.addBorder()
			.build();
	}
	
	private void initUI()
	{
		challenge = new Image();
		SimpleImageSource src = new SimpleImageSource(engine.getImage());
		challenge.setSource(src.getResource());
		answer = new TextFieldWithContextLabel(showLabelInline);
		answer.setLabel(msg.getMessage("CaptchaComponent.answer"));
		resetChallenge = new Button();
		resetChallenge.setStyleName(Styles.vButtonSmall.toString());
		resetChallenge.addStyleName("u-captcha-reset");
		resetChallenge.setDescription(msg.getMessage("CaptchaComponent.resetDesc"));
		resetChallenge.setIcon(Images.refresh.getResource());
		resetChallenge.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				reset();
			}
		});
	}
	
	public Focusable getFocussTarget()
	{
		return answer;
	}
	
	public void reset()
	{
		initEngine();
		SimpleImageSource src = new SimpleImageSource(engine.getImage());
		challenge.setSource(src.getResource());
		answer.setValue("");
	}
	
	public void resetFull()
	{
		reset();
		answer.setComponentError(null);
	}
	
	private HorizontalLayout createCapchaLine()
	{
		HorizontalLayout captchaLine = new HorizontalLayout();
		captchaLine.addComponents(challenge, resetChallenge);
		captchaLine.setMargin(false);
		captchaLine.setComponentAlignment(resetChallenge, Alignment.MIDDLE_LEFT);
		captchaLine.setExpandRatio(challenge, 2);
		return captchaLine;
	}

	/**
	 * Create and return UI.
	 */
	public Component getAsComponent()
	{
		return getAsComponent(Alignment.MIDDLE_LEFT);
	}
	
	/**
	 * Create and return UI.
	 */
	public Component getAsComponent(Alignment answerAligment)
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		HorizontalLayout capchaLine = createCapchaLine();
		ret.addComponents(capchaLine, answer);
		ret.setComponentAlignment(capchaLine, answerAligment);
		ret.setComponentAlignment(answer, answerAligment);
		return ret;
	}

	public Component getAsFullWidthComponent()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setWidth(100, Unit.PERCENTAGE);
		HorizontalLayout capchaLine = createCapchaLine();
		ret.addComponents(capchaLine, answer);
		capchaLine.setWidth(100, Unit.PERCENTAGE);
		answer.setWidth(100, Unit.PERCENTAGE);
		return ret;
	}
	
	/**
	 * Create a UI and adds it to the provided {@link FormLayout}.
	 * @param parent
	 */
	public void addToFormLayout(FormLayout parent)
	{
		parent.addComponents(createCapchaLine(), answer);
	}
	
	/**
	 * Checks if the value entered in answer text field is the same as the one on captcha image.
	 * If so then nothing more is done. If not then exception is raised and the captcha is regenerated. 
	 * @throws WrongArgumentException
	 */
	public void verify() throws WrongArgumentException
	{
		String attempt = answer.getValue();
		if (attempt == null)
		{
			reset();
			answer.setComponentError(new UserError(msg.getMessage("CaptchaComponent.wrongAnswer")));
			throw new WrongArgumentException("");
		}
		String rightAnswer = engine.getAnswer().toLowerCase();
		attempt = attempt.toLowerCase();
		if (!rightAnswer.equals(attempt))
		{
			reset();
			answer.setComponentError(new UserError(msg.getMessage("CaptchaComponent.wrongAnswer")));
			throw new WrongArgumentException("");
		}
		answer.setComponentError(null);
	}
	
	
	public static class SimpleImageSource implements StreamSource
	{
		private static final long serialVersionUID = 1L;
		private final byte[] isData;
		
		public SimpleImageSource(BufferedImage value)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
			try
			{
				ImageIO.write(value, "png", bos);
			} catch (IOException e)
			{
				throw new InternalException("Image can not be encoded as PNG", e);
			}
		        isData = bos.toByteArray(); 
		}
		
		@Override
		public InputStream getStream()
		{
			return new ByteArrayInputStream(isData);
		}
		
		public Resource getResource()
		{
			return new StreamResource(this, "imgattribute-"+random.nextLong()
					+".png");
		}
	}
}
