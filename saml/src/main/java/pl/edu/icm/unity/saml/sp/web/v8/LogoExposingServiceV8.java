/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web.v8;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.saml.metadata.cfg.ExternalLogoFileLoader;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.web.IdPVisalSettings;
import pl.edu.icm.unity.webui.authn.IdPAuthNComponent;
import pl.edu.icm.unity.webui.common.FileStreamResource;

import java.net.URI;

@Component
class LogoExposingServiceV8
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, LogoExposingServiceV8.class);
	
	private final ExternalLogoFileLoader externalLogoFileLoader;
	private final URIAccessService uriAccessService;
	
	LogoExposingServiceV8(ExternalLogoFileLoader externalLogoFileLoader, URIAccessService uriAccessService)
	{
		this.externalLogoFileLoader = externalLogoFileLoader;
		this.uriAccessService = uriAccessService;
	}

	Resource getAsResource(IdPVisalSettings configuration, TrustedIdPKey configKey)
	{
		if (configuration.getV8LogoURI() == null)
			return null;
		return (configuration.federationId == null || configuration.getV8LogoURI().startsWith("file:")) ?
			getDirectlyDefinedImage(configuration) :
			getPrefetchedFederationLogo(configuration, configKey);
	}

	private Resource getPrefetchedFederationLogo(IdPVisalSettings configuration, TrustedIdPKey configKey)
	{
		try
		{
			return externalLogoFileLoader.getFile(configuration.federationId, configKey,
					VaadinService.getCurrentRequest().getLocale())
				.map(IdPAuthNComponent.DisappearingFileResource::new)
				.orElse(null);
		} catch (Exception e)
		{
			log.debug("Can not load logo fetched from URI " + configuration.getV8LogoURI(), e);
			return null;
		}
	}

	private Resource getDirectlyDefinedImage(IdPVisalSettings configuration)
	{
		try
		{
			URI uri = URIHelper.parseURI(configuration.getV8LogoURI());
			return URIHelper.isWebReady(uri) ? 
					new ExternalResource(uri.toString()) : 
					new FileStreamResource(uriAccessService.readImageURI(uri, UI.getCurrent().getTheme()))
						.getResource();
		} catch (IllegalURIException e)
		{
			log.warn("Invalid logo URI {}", configuration.getV8LogoURI(), e);
			return null;
		}
	}
}
