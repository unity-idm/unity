/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.saml.sp.SAMLVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * Factory for {@link SAMLAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component
class SAMLAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private MessageSource msg;
	private InputTranslationProfileFieldFactory profileFieldFactory;
	private RegistrationsManagement registrationMan;
	private PKIManagement pkiMan;
	private RealmsManagement realmMan;
	private IdentityTypesRegistry idTypesReg;
	private FileStorageService fileStorageService;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private ImageAccessService imageAccessService;

	@Autowired
	SAMLAuthenticatorEditorFactory(MessageSource msg, UnityServerConfiguration serverConfig,
			RegistrationsManagement registrationMan, RealmsManagement realmMan, PKIManagement pkiMan,
			IdentityTypesRegistry idTypesReg, InputTranslationProfileFieldFactory profileFieldFactory,
			FileStorageService fileStorageService, URIAccessService uriAccessService,
			ImageAccessService imageAccessService)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.realmMan = realmMan;
		this.idTypesReg = idTypesReg;
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		this.imageAccessService = imageAccessService;

	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return SAMLVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new SAMLAuthenticatorEditor(msg, serverConfig, pkiMan, profileFieldFactory, registrationMan,
				realmMan, idTypesReg, fileStorageService, uriAccessService, imageAccessService);
	}

}
