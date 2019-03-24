/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.pam.web;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.EditInputTranslationProfileSubViewHelper;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.pam.PAMVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link PamAuthenticatorEditor}
 * @author P.Piernik
 *
 */
@Component
public class PamAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{

	private UnityMessageSource msg;
	private RegistrationsManagement regMan;
	private EditInputTranslationProfileSubViewHelper profileHelper;

	@Autowired
	public PamAuthenticatorEditorFactory(UnityMessageSource msg, RegistrationsManagement regMan,
			EditInputTranslationProfileSubViewHelper profileHelper)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.profileHelper = profileHelper;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return PAMVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new PamAuthenticatorEditor(msg,
				regMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				profileHelper);
	}

}
