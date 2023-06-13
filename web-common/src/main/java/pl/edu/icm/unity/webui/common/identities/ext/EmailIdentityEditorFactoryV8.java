/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorFactory;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

/**
 * Produces {@link EmailIdentityEditor} instances.
 * 
 * @author P. Piernik
 */
@Component
public class EmailIdentityEditorFactoryV8 implements IdentityEditorFactory
{
	private MessageSource msg;
	private EmailConfirmationManager emailConfirmationMan;
	private EntityResolver idResolver;
	private ConfirmationInfoFormatter formatter;

	@Autowired
	public EmailIdentityEditorFactoryV8(MessageSource msg,
	                                    EmailConfirmationManager emailConfirmationMan, EntityResolver idResolver,
	                                    ConfirmationInfoFormatter formatter)
	{
		this.msg = msg;
		this.emailConfirmationMan = emailConfirmationMan;
		this.idResolver = idResolver;
		this.formatter = formatter;
	}

	@Override
	public String getSupportedIdentityType()
	{
		return EmailIdentity.ID;
	}

	@Override
	public IdentityEditor createInstance()
	{
		return new EmailIdentityEditor(msg, emailConfirmationMan, idResolver, formatter);
	}
}
