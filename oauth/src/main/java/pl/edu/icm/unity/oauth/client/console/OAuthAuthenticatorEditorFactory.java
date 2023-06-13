/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.oauth.client.OAuth2Verificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * Factory for {@link OAuthAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component
class OAuthAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;
	private final InputTranslationProfileFieldFactory profileFieldFactory;
	private final RegistrationsManagement registrationMan;
	private final PKIManagement pkiMan;
	private final FileStorageService fileStorageService;
	private final URIAccessService uriAccessService;
	private final UnityServerConfiguration serverConfig;
	private final AdvertisedAddressProvider advertisedAddrProvider;
	private final ImageAccessService imageAccessService;

	@Autowired
	OAuthAuthenticatorEditorFactory(MessageSource msg,
			RegistrationsManagement registrationMan,
			PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory,
			FileStorageService fileStorageService,
			URIAccessService uriAccessService,
			ImageAccessService imageAccessService,
			UnityServerConfiguration serverConfig,
			AdvertisedAddressProvider advertisedAddrProvider)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.imageAccessService = imageAccessService;
		this.serverConfig = serverConfig;
		this.advertisedAddrProvider = advertisedAddrProvider;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return OAuth2Verificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new OAuthAuthenticatorEditor(msg, serverConfig, pkiMan, fileStorageService, uriAccessService, 
				imageAccessService,
				profileFieldFactory, registrationMan, advertisedAddrProvider);
	}
}
