/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.identities.username;

import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

@Component
public class UsernameIdentityEditorFactoryV23 implements IdentityEditorFactory
{
	private final MessageSource msg;
	
	@Autowired
	public UsernameIdentityEditorFactoryV23(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedIdentityType()
	{
		return UsernameIdentity.ID;
	}

	@Override
	public IdentityEditor createInstance()
	{
		return new UsernameIdentityEditor(msg);
	}
}
