/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console;

import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.saml.sp.SAMLVerificator;

@Component
class SAMLAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;
	private final InputTranslationProfileFieldFactory profileFieldFactory;
	private final RegistrationsManagement registrationMan;
	private final PKIManagement pkiMan;
	private final RealmsManagement realmMan;
	private final IdentityTypesRegistry idTypesReg;
	private final FileStorageService fileStorageService;
	private final URIAccessService uriAccessService;
	private final UnityServerConfiguration serverConfig;
	private final VaadinLogoImageLoader imageAccessService;
	private final NotificationPresenter notificationPresenter;
	private final SharedEndpointManagement sharedEndpointManagement;
	private final HtmlTooltipFactory htmlTooltipFactory;

	SAMLAuthenticatorEditorFactory(MessageSource msg, UnityServerConfiguration serverConfig,
			RegistrationsManagement registrationMan, RealmsManagement realmMan, PKIManagement pkiMan,
			IdentityTypesRegistry idTypesReg, InputTranslationProfileFieldFactory profileFieldFactory,
			FileStorageService fileStorageService, URIAccessService uriAccessService,
			VaadinLogoImageLoader imageAccessService, NotificationPresenter notificationPresenter,
			SharedEndpointManagement sharedEndpointManagement, HtmlTooltipFactory htmlTooltipFactory)
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
		this.notificationPresenter = notificationPresenter;
		this.sharedEndpointManagement = sharedEndpointManagement;
		this.htmlTooltipFactory = htmlTooltipFactory;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return SAMLVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new SAMLAuthenticatorEditor(msg, htmlTooltipFactory, serverConfig, pkiMan, profileFieldFactory, registrationMan,
				realmMan, idTypesReg, fileStorageService, uriAccessService, imageAccessService, notificationPresenter,
				sharedEndpointManagement);
	}

}
