/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;

public class EmailConfirmationConfigurationViewer extends VerticalLayout
{
	private MessageSource msg;
	private Label msgTemplate;
	private Label validityTime;

	public EmailConfirmationConfigurationViewer(MessageSource msg)
	{
		super();
		setSpacing(false);
		setMargin(false);
		this.msg = msg;
		msgTemplate = new Label();
		add(msgTemplate);
		validityTime = new Label();
		add(validityTime);
	}

	public EmailConfirmationConfigurationViewer(MessageSource msg,
	                                            EmailConfirmationConfiguration init)
	{

		this(msg);
		setValue(init);
	}

	public void setValue(EmailConfirmationConfiguration init)
	{
		String msgTemplateName = init != null ? init.getMessageTemplate() : null;
		msgTemplate.setText(msg
				.getMessage("EmailConfirmationConfiguration.confirmationMsgTemplate")
				+ " " + (msgTemplateName != null ? msgTemplateName : ""));

		validityTime.setText(msg.getMessage("EmailConfirmationConfiguration.validityTime")
				+ " "
				+ (init != null ? String.valueOf(init.getValidityTime()) : ""));
	}

}