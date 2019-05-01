/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.web.authnEditor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.sp.SAMLVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link SAMLAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component
class SAMLAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private UnityMessageSource msg;
	private InputTranslationProfileFieldFactory profileFieldFactory;
	private RegistrationsManagement registrationMan;
	private PKIManagement pkiMan;
	private RealmsManagement realmMan;
	private IdentityTypesRegistry idTypesReg;
	private FileStorageService fileStorageService;

	@Autowired
	SAMLAuthenticatorEditorFactory(UnityMessageSource msg, RegistrationsManagement registrationMan,
			RealmsManagement realmMan, PKIManagement pkiMan, IdentityTypesRegistry idTypesReg,
			InputTranslationProfileFieldFactory profileFieldFactory,
			FileStorageService fileStorageService)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.realmMan = realmMan;
		this.idTypesReg = idTypesReg;
		this.fileStorageService = fileStorageService;
	

	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return SAMLVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new SAMLAuthenticatorEditor(msg, pkiMan, profileFieldFactory, registrationMan, realmMan,
				idTypesReg, fileStorageService);
	}

}
