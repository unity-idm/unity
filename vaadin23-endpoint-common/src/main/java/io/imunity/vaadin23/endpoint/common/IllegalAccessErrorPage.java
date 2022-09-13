/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import io.imunity.vaadin23.elements.NotificationPresenter;
import pl.edu.icm.unity.MessageSource;

import javax.servlet.http.HttpServletResponse;

@Tag(Tag.DIV)
public class IllegalAccessErrorPage extends Component implements HasErrorParameter<IllegalAccessException>
{
	private final MessageSource msg;
	private final Vaddin23WebLogoutHandler logoutHandler;
	public IllegalAccessErrorPage(MessageSource messageSource, Vaddin23WebLogoutHandler logoutHandler)
	{
		this.msg = messageSource;
		this.logoutHandler = logoutHandler;
	}

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<IllegalAccessException> parameter)
	{
		UI.getCurrent().access(() -> {
			NotificationPresenter.showCriticalError(logoutHandler::logout, msg.getMessage("ProjectController.noProjectAvailable"), null);
		});
		return HttpServletResponse.SC_FORBIDDEN;
	}
}
