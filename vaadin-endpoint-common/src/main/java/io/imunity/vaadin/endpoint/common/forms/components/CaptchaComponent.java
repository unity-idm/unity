/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.forms.components;

import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import nl.captcha.Captcha;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.text.producer.DefaultTextProducer;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;

import java.awt.*;

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
	private final MessageSource msg;
	private final boolean showLabelInline;
	private Captcha engine;
	
	private Image challenge;
	private TextField answer;
	private Button resetChallenge;

	private final int length;

	public CaptchaComponent(MessageSource msg, int length, boolean showLabelInline)
	{
		this.msg = msg;
		this.showLabelInline = showLabelInline;
		this.length = length;
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
		if(showLabelInline)
			answer.setPlaceholder(msg.getMessage("CaptchaComponent.answer"));
		else
			answer.setLabel(msg.getMessage("CaptchaComponent.answer"));
		answer.setWidthFull();
		resetChallenge = new Button();
		resetChallenge.addClassName("u-captcha-reset");
		resetChallenge.setTooltipText(msg.getMessage("CaptchaComponent.resetDesc"));
		resetChallenge.setIcon(VaadinIcon.REFRESH.create());
		resetChallenge.getStyle().set("background-color", "transparent");
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
		answer.setInvalid(false);
	}
	
	private HorizontalLayout createCapchaLine()
	{
		HorizontalLayout captchaLine = new HorizontalLayout();
		captchaLine.add(challenge, resetChallenge);
		captchaLine.setMargin(false);
		captchaLine.setAlignItems(FlexComponent.Alignment.CENTER);
		captchaLine.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		captchaLine.setWidthFull();
		return captchaLine;
	}

	public VerticalLayout getAsComponent()
	{
		return getAsComponent(FlexComponent.Alignment.START);
	}

	public void setInvalid()
	{
		answer.setInvalid(true);
	}

	public VerticalLayout getAsComponent(FlexComponent.Alignment answerAlignment)
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setPadding(false);
		HorizontalLayout capchaLine = createCapchaLine();
		ret.add(capchaLine, answer);
		ret.setAlignItems(answerAlignment);
		return ret;
	}

	public void verify() throws WrongArgumentException
	{
		String attempt = answer.getValue();
		if (attempt == null)
		{
			reset();
			answer.setInvalid(true);
			answer.setErrorMessage(msg.getMessage("CaptchaComponent.wrongAnswer"));
			throw new WrongArgumentException("");
		}
		String rightAnswer = engine.getAnswer().toLowerCase();
		attempt = attempt.toLowerCase();
		if (!rightAnswer.equals(attempt))
		{
			reset();
			answer.setInvalid(true);
			answer.setErrorMessage(msg.getMessage("CaptchaComponent.wrongAnswer"));
			throw new WrongArgumentException("");
		}
		answer.setInvalid(false);
	}
}
