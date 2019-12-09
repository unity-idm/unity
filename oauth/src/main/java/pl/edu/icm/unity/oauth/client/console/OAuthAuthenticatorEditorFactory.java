/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
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
	private UnityMessageSource msg;
	private InputTranslationProfileFieldFactory profileFieldFactory;
	private RegistrationsManagement registrationMan;
	private PKIManagement pkiMan;
	private FileStorageService fileStorageService;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private NetworkServer networkServer;
	private ImageAccessService imageAccessService;

	@Autowired
	OAuthAuthenticatorEditorFactory(UnityMessageSource msg, RegistrationsManagement registrationMan,
			PKIManagement pkiMan, InputTranslationProfileFieldFactory profileFieldFactory,
			FileStorageService fileStorageService, URIAccessService uriAccessService, 
			ImageAccessService imageAccessService, UnityServerConfiguration serverConfig, 
			NetworkServer networkServer)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.imageAccessService = imageAccessService;
		this.serverConfig = serverConfig;
		this.networkServer = networkServer;
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
				profileFieldFactory, registrationMan, networkServer::getAdvertisedAddress);
	}
}
