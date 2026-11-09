/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.forms.components;

import java.awt.Color;

import org.springframework.stereotype.Component;

import nl.captcha.Captcha;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.text.producer.DefaultTextProducer;

@Component
public class CaptchaImageFactory
{
	private static final char[] CAPTCHA_CHARS = "qwertyuipasdfghjklzxcvbnm123456789".toCharArray();

	public Captcha create(int length)
	{
		return new Captcha.Builder(31 * length, 50)
				.addText(new DefaultTextProducer(length, CAPTCHA_CHARS))
				.addBackground(new GradiatedBackgroundProducer(new Color(0xf5, 0x92, 0x01),
						new Color(0xE0, 0xE0, 0xE0)))
				.gimp(new FishEyeGimpyRenderer(Color.gray, Color.gray))
				.addBorder()
				.build();
	}
}
