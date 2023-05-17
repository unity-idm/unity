/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.config;

import eu.emi.security.authn.x509.X509Credential;

public class AdditionalCredential
{
	public final String name;
	public final X509Credential credential;

	public AdditionalCredential(String name, X509Credential credential)
	{
		this.name = name;
		this.credential = credential;
	}

}
