/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;

public class UnsuccessfulAuthenticationHelper
{
	public static boolean failedAttemptsExceeded()
	{
		String clientIp = HTTPRequestContext.getCurrent().getClientIP();
		UnsuccessfulAuthenticationCounter counter = VaadinWebLogoutHandler.getLoginCounter();
		if (counter.getRemainingBlockedTime(clientIp) > 0)
			return true;
		return false;
	}
}
