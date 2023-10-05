/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import com.vaadin.flow.router.*;
import io.imunity.console.ConsoleMenu;
import io.imunity.vaadin.elements.Breadcrumb;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.icm.unity.base.message.MessageSource;

import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;

@PermitAll
@Breadcrumb(key = "error")
@ParentLayout(ConsoleMenu.class)
public class ConsoleErrorPage extends ConsoleViewComponent implements HasErrorParameter<Exception>
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final MessageSource messageSource;

	ConsoleErrorPage(MessageSource messageSource)
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
}
