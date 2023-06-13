/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Set;

import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;

/**
 * Default credential requirements. Contains all configured credential.
 * @author P.Piernik
 *
 */
public class SystemAllCredentialRequirements extends CredentialRequirements
{
	public static final String NAME = CredentialRequirementManagement.DEFAULT_CREDENTIAL_REQUIREMENT;

	public SystemAllCredentialRequirements(MessageSource msg, Set<String> allCredentials)
	{
		super(NAME, msg.getMessage("CredentialRequirements.systemCredentialRequirements.desc"), allCredentials);
		setReadOnly(true);
	}
}