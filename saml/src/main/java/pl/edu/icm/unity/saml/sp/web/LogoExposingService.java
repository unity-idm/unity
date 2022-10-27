/**********************************************************************
 *                     Copyright (c) 2022, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.saml.sp.web;

import java.net.URI;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.saml.metadata.cfg.ExternalLogoFileLoader;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.webui.authn.IdPAuthNComponent;
import pl.edu.icm.unity.webui.common.FileStreamResource;

@Component
class LogoExposingService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, LogoExposingService.class);
	
	private final ExternalLogoFileLoader externalLogoFileLoader;
	private final URIAccessService uriAccessService;
	
	LogoExposingService(ExternalLogoFileLoader externalLogoFileLoader, URIAccessService uriAccessService)
	{
		this.externalLogoFileLoader = externalLogoFileLoader;
		this.uriAccessService = uriAccessService;
	}

	Resource getAsResource(IdPVisalSettings configuration, TrustedIdPKey configKey)
	{
		if (configuration.logoURI == null)
			return null;
		return (configuration.federationId == null || configuration.logoURI.startsWith("file:")) ?
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
			log.debug("Can not load logo fetched from URI " + configuration.logoURI, e);
			return null;
		}
	}

	private Resource getDirectlyDefinedImage(IdPVisalSettings configuration)
	{
		try
		{
			URI uri = URIHelper.parseURI(configuration.logoURI);
			return URIHelper.isWebReady(uri) ? 
					new ExternalResource(uri.toString()) : 
					new FileStreamResource(uriAccessService.readImageURI(uri, UI.getCurrent().getTheme()))
						.getResource();
		} catch (IllegalURIException e)
		{
			log.warn("Invalid logo URI {}", configuration.logoURI, e);
			return null;
		}
	}
}
