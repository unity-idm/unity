/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;

import io.imunity.upman.front.views.UpManMenu;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;

@PermitAll
@ParentLayout(UpManMenu.class)
public class UpmanErrorPage extends Composite<Div> implements HasErrorParameter<Exception>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UpmanErrorPage.class);

	private final MessageSource messageSource;
	UpmanErrorPage(MessageSource messageSource)
	{
		this.messageSource = messageSource;
	}

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter)
	{
		log.error("Vaadin rendering error:", parameter.getCaughtException());
		getElement().setText(messageSource.getMessage("Error"));
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}
}
