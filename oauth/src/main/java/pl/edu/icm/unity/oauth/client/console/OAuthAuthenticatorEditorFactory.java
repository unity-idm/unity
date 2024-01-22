/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.oauth.client.OAuth2Verificator;


@Component
class OAuthAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;
	private final InputTranslationProfileFieldFactory profileFieldFactory;
	private final RegistrationsManagement registrationMan;
	private final PKIManagement pkiMan;
	private final FileStorageService fileStorageService;
	private final AdvertisedAddressProvider advertisedAddrProvider;
	private final VaadinLogoImageLoader imageAccessService;
	private final UnityServerConfiguration serverConfig;
	private final NotificationPresenter notificationPresenter;

	OAuthAuthenticatorEditorFactory(MessageSource msg,
			RegistrationsManagement registrationMan,
			PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory,
			FileStorageService fileStorageService,
			VaadinLogoImageLoader imageAccessService,
			AdvertisedAddressProvider advertisedAddrProvider,
			UnityServerConfiguration serverConfig,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.fileStorageService = fileStorageService;
		this.imageAccessService = imageAccessService;
		this.notificationPresenter = notificationPresenter;
		this.advertisedAddrProvider = advertisedAddrProvider;
		this.serverConfig = serverConfig;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return OAuth2Verificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance()
	{
		return new OAuthAuthenticatorEditor(msg, pkiMan, fileStorageService,
				imageAccessService, profileFieldFactory, registrationMan, advertisedAddrProvider,
				serverConfig, notificationPresenter);
	}
}
