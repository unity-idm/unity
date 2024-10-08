/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.SessionReinitializer;

import jakarta.servlet.http.HttpSession;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;

public class VaadinSessionReinitializer implements SessionReinitializer
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, VaadinSessionReinitializer.class);

	@Override
	public HttpSession reinitialize()
	{
		VaadinSession vss = VaadinSession.getCurrent();
		if (vss == null)
		{
			LOG.error("BUG: Can't get VaadinSession to reinitialize session.");
			throw new IllegalStateException("AuthenticationProcessor.authnInternalError");
		}
		LOG.debug("Vaadin session {} reinitialization", vss.getSession().getId());
		VaadinService.reinitializeSession(VaadinService.getCurrentRequest());
		((WrappedHttpSession) vss.getSession()).getHttpSession()
				.removeAttribute(LoginToHttpSessionBinder.SELF_REFERENCING_ATTRIBUTE);
		return ((WrappedHttpSession) vss.getSession()).getHttpSession();
	}
}
