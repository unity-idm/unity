/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Set;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Default credential requirements. Contains all configured credential.
 * @author P.Piernik
 *
 */
public class SystemAllCredentialRequirements extends CredentialRequirements
{
	public static final String NAME = "sys:all";

	public SystemAllCredentialRequirements(UnityMessageSource msg, Set<String> allCredentials)
	{
		super(NAME, msg.getMessage("CredentialRequirements.systemCredentialRequirements.desc"), allCredentials);
		setReadOnly(true);
	}
}