/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorFactory;

/**
 * Produces {@link EmailIdentityEditor} instances.
 * @author P. Piernik
 */
@Component
public class EmailIdentityEditorFactory implements IdentityEditorFactory
{
	private UnityMessageSource msg;
	
	@Autowired
	public EmailIdentityEditorFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedIdentityType()
	{
		return EmailIdentity.ID;
	}

	@Override
	public IdentityEditor createInstance()
	{
		return new EmailIdentityEditor(msg);
	}
}
