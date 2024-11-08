/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import java.security.cert.X509Certificate;

public class KeyIdExtractor
{
	public static String getKeyId(X509Certificate cert)
	{
		return cert.getSerialNumber()
				.toString(10);
	}

}
