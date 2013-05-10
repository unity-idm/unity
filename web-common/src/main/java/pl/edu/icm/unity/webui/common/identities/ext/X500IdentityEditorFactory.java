/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorFactory;

/**
 * Produces {@link X500IdentityEditor} instances.
 * @author K. Benedyczak
 */
@Component
public class X500IdentityEditorFactory implements IdentityEditorFactory
{
	private UnityMessageSource msg;
	
	@Autowired
	public X500IdentityEditorFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedIdentityType()
	{
		return X500Identity.ID;
	}

	@Override
	public IdentityEditor createInstance()
	{
		return new X500IdentityEditor(msg);
	}
}
