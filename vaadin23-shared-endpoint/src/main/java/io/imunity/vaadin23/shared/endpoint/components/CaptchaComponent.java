/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.StreamResource;
import nl.captcha.Captcha;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.text.producer.DefaultTextProducer;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

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
	private MessageSource msg;
	private Captcha engine;
	
	private Image challenge;
	private TextField answer;
	private Button resetChallenge;

	private int length;
	private boolean showLabelInline;
	
	public CaptchaComponent(MessageSource msg, boolean showLabelInline)
	{
		this(msg, 6, showLabelInline);
	}

	public CaptchaComponent(MessageSource msg, int length, boolean showLabelInline)
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
		challenge.setSrc(src.getResource());
		answer = new TextField();
		answer.setLabel(msg.getMessage("CaptchaComponent.answer"));
		resetChallenge = new Button();
		resetChallenge.addClassName("u-captcha-reset");
		resetChallenge.getElement().setProperty("title", msg.getMessage("CaptchaComponent.resetDesc"));
		resetChallenge.setIcon(VaadinIcon.REFRESH.create());
		resetChallenge.addClickListener(event -> reset());
	}
	
	public Focusable<TextField> getFocusTarget()
	{
		return answer;
	}
	
	public void reset()
	{
		initEngine();
		SimpleImageSource src = new SimpleImageSource(engine.getImage());
		challenge.setSrc(src.getResource());
		answer.setValue("");
	}
	
	public void resetFull()
	{
		reset();
		answer.setErrorMessage(null);
	}
	
	private HorizontalLayout createCapchaLine()
	{
		HorizontalLayout captchaLine = new HorizontalLayout();
		captchaLine.add(challenge, resetChallenge);
		captchaLine.setMargin(false);
		captchaLine.setAlignItems(FlexComponent.Alignment.CENTER);
		return captchaLine;
	}

	public Component getAsComponent()
	{
		return getAsComponent(FlexComponent.Alignment.START);
	}

	public void setInvalid()
	{
		answer.setInvalid(true);
	}

	public Component getAsComponent(FlexComponent.Alignment answerAlignment)
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setPadding(false);
		HorizontalLayout capchaLine = createCapchaLine();
		ret.add(capchaLine, answer);
		ret.setAlignItems(answerAlignment);
		return ret;
	}

	public Component getAsFullWidthComponent()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setWidthFull();
		HorizontalLayout capchaLine = createCapchaLine();
		ret.add(capchaLine, answer);
		capchaLine.setWidthFull();
		answer.setWidthFull();
		return ret;
	}
	

	public void addToFormLayout(FormLayout parent)
	{
		parent.add(createCapchaLine(), answer);
	}

	public void verify() throws WrongArgumentException
	{
		String attempt = answer.getValue();
		if (attempt == null)
		{
			reset();
			answer.setErrorMessage(msg.getMessage("CaptchaComponent.wrongAnswer"));
			throw new WrongArgumentException("");
		}
		String rightAnswer = engine.getAnswer().toLowerCase();
		attempt = attempt.toLowerCase();
		if (!rightAnswer.equals(attempt))
		{
			reset();
			answer.setErrorMessage(msg.getMessage("CaptchaComponent.wrongAnswer"));
			throw new WrongArgumentException("");
		}
		answer.setErrorMessage(null);
	}
	
	
	public static class SimpleImageSource
	{
		private final byte[] data;
		
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
		        data = bos.toByteArray();
		}
		
		public StreamResource getResource()
		{
			return new StreamResource("imgattribute-"+random.nextLong()+".png", () -> new ByteArrayInputStream(data));
		}
	}
}
