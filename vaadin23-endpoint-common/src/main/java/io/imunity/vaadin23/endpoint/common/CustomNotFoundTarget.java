/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteNotFoundError;
import pl.edu.icm.unity.MessageSource;

import javax.servlet.http.HttpServletResponse;

public class CustomNotFoundTarget extends RouteNotFoundError
{
	public final MessageSource messageSource;
	public CustomNotFoundTarget(MessageSource messageSource)
	{
		this.messageSource = messageSource;
	}

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter)
	{
		getElement().setText(messageSource.getMessage("NavigationError"));
		return HttpServletResponse.SC_NOT_FOUND;
	}
}
