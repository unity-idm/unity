/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.username;

import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

@Component
public class UsernameIdentityEditorFactory implements IdentityEditorFactory
{
	private final MessageSource msg;
	
	@Autowired
	public UsernameIdentityEditorFactory(MessageSource msg)
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
