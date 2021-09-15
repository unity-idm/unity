/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.SessionReinitializer;

public class VaadinFriendlySessionReinitializer implements SessionReinitializer
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, VaadinFriendlySessionReinitializer.class);

	@Override
	public HttpSession reinitialize()
	{
		VaadinSession vss = VaadinSession.getCurrent();
		if (vss == null)
		{
			LOG.error("BUG: Can't get VaadinSession to reinitialize session.");
			throw new IllegalStateException("AuthenticationProcessor.authnInternalError");
		}
		LOG.debug("Vaadin friendly session reinitialization.");
		VaadinService.reinitializeSession(VaadinService.getCurrentRequest());
		return ((WrappedHttpSession) vss.getSession()).getHttpSession();
	}
}
