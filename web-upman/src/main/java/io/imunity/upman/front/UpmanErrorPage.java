/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import io.imunity.upman.front.views.UpManMenu;
import pl.edu.icm.unity.base.message.MessageSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;

@PermitAll
@ParentLayout(UpManMenu.class)
public class UpmanErrorPage extends UpmanViewComponent implements HasErrorParameter<Exception>
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final MessageSource messageSource;
	UpmanErrorPage(MessageSource messageSource)
	{
		this.messageSource = messageSource;
	}

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter)
	{
		LOG.error("Vaadin rendering error:", parameter.getCaughtException());
		getElement().setText(messageSource.getMessage("Error"));
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}

	@Override
	public void loadData()
	{
	}
}
