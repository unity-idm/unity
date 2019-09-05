/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.strter;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.server.UnityApplication;

public class UnityTestServer
{
	public static void main(String... args)
	{
		UnityApplication theServer = new UnityApplication(UnityMessageSource.PROFILE_FAIL_ON_MISSING);
		theServer.run(new String[] { "src/test/resources/unityServer.conf" });
	}
}
