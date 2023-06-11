/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.identifier;

import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;

@Component
public class IdentifierIdentityEditorFactory implements IdentityEditorFactory
{
	private final MessageSource msg;
	
	@Autowired
	public IdentifierIdentityEditorFactory(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedIdentityType()
	{
		return IdentifierIdentity.ID;
	}

	@Override
	public IdentityEditor createInstance()
	{
		return new IdentifierIdentityEditor(msg);
	}
}
