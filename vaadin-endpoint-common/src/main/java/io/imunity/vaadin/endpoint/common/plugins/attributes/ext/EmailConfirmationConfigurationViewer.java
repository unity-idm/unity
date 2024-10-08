/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;

class EmailConfirmationConfigurationViewer extends VerticalLayout
{
	private final MessageSource msg;
	private final Span msgTemplate;
	private final Span validityTime;

	EmailConfirmationConfigurationViewer(MessageSource msg)
	{
		super();
		setSpacing(false);
		setMargin(false);
		this.msg = msg;
		msgTemplate = new Span();
		add(msgTemplate);
		validityTime = new Span();
		add(validityTime);
	}

	EmailConfirmationConfigurationViewer(MessageSource msg,
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
