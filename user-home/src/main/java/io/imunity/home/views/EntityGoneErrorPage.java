/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.exceptions.EntityNotFoundException;

/**
 * Catches the storage layer's unchecked {@link EntityNotFoundException} without depending on {@link HomeUiMenu} as
 * parent layout, unlike {@link HomeErrorPage}. This matters when the current session's own entity
 * was concurrently removed (e.g. self-deletion from another tab): HomeUiMenu itself queries data
 * for that entity and would throw the same exception while Vaadin tries to build it as this
 * page's parent, so the friendlier error page could never render. Other {@code IllegalArgumentException}s are
 * left to {@link HomeErrorPage}, so that unrelated failures do not log the user out.
 */
@PermitAll
public class EntityGoneErrorPage extends Composite<Div> implements HasErrorParameter<EntityNotFoundException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EntityGoneErrorPage.class);

	private final MessageSource messageSource;
	private final VaadinWebLogoutHandler logoutHandler;

	EntityGoneErrorPage(MessageSource messageSource, VaadinWebLogoutHandler logoutHandler)
	{
		this.messageSource = messageSource;
		this.logoutHandler = logoutHandler;
	}

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<EntityNotFoundException> parameter)
	{
		log.warn("The account backing the current session no longer exists, logging out", parameter.getCaughtException());
		getElement().setText(messageSource.getMessage("Error"));
		logoutHandler.logoutImmediately("/");
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}
}
