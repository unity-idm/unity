/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.column;

import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;

public class UnsuccessfulAuthenticationHelper
{
	public static boolean failedAttemptsExceeded()
	{
		String clientIp = HTTPRequestContext.getCurrent().getClientIP();
		UnsuccessfulAuthenticationCounter counter = StandardWebLogoutHandler.getLoginCounter();
		if (counter.getRemainingBlockedTime(clientIp) > 0)
			return true;
		return false;
	}
}
