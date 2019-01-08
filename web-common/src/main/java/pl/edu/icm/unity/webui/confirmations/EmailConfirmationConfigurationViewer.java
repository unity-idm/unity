/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.confirmations;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;

/**
 * Simple shows {@link EmailConfirmationConfiguration}
 * @author P.Piernik
 *
 */
public class EmailConfirmationConfigurationViewer extends VerticalLayout
{
	private UnityMessageSource msg;
	private Label msgTemplate;
	private Label validityTime;

	public EmailConfirmationConfigurationViewer(UnityMessageSource msg)
	{
		super();
		setSpacing(false);
		setMargin(false);
		this.msg = msg;
		msgTemplate = new Label();
		addComponent(msgTemplate);
		validityTime = new Label();
		addComponent(validityTime);
	}

	public EmailConfirmationConfigurationViewer(UnityMessageSource msg,
			EmailConfirmationConfiguration init)
	{

		this(msg);
		setValue(init);
	}

	public void setValue(EmailConfirmationConfiguration init)
	{
		String msgTemplateName = init != null ? init.getMessageTemplate() : null;
		msgTemplate.setValue(msg
				.getMessage("EmailConfirmationConfiguration.confirmationMsgTemplate")
				+ " " + (msgTemplateName != null ? msgTemplateName : ""));

		validityTime.setValue(msg.getMessage("EmailConfirmationConfiguration.validityTime")
				+ " "
				+ (init != null ? String.valueOf(init.getValidityTime()) : ""));
	}

}
