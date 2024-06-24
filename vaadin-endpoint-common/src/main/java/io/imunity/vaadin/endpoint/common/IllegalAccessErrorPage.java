/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;

import jakarta.servlet.http.HttpServletResponse;

@PermitAll
@Tag(Tag.DIV)
public class IllegalAccessErrorPage extends Component implements HasErrorParameter<IllegalAccessException>
{
	private final MessageSource msg;
	private final VaadinWebLogoutHandler logoutHandler;
	public IllegalAccessErrorPage(MessageSource messageSource, VaadinWebLogoutHandler logoutHandler)
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
