/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorFactory;

/**
 * Produces {@link UsernameIdentityEditor} instances.
 * @author K. Benedyczak
 */
@Component
public class UsernameIdentityEditorFactoryV8 implements IdentityEditorFactory
{
	private MessageSource msg;
	
	@Autowired
	public UsernameIdentityEditorFactoryV8(MessageSource msg)
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
