/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorFactory;

/**
 * Produces {@link IdentifierIdentityEditor} instances.
 * @author K. Benedyczak
 */
@Component
public class IdentifierIdentityEditorFactoryV8 implements IdentityEditorFactory
{
	private MessageSource msg;
	
	@Autowired
	public IdentifierIdentityEditorFactoryV8(MessageSource msg)
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
