/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.saml.metadata.cfg.ExternalLogoFileLoader;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.vaadin.flow.server.VaadinService.getCurrentRequest;
import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE;

@Component
class LogoExposingService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, LogoExposingService.class);
	
	private final ExternalLogoFileLoader externalLogoFileLoader;
	private final VaadinLogoImageLoader logoImageLoader;
	
	LogoExposingService(ExternalLogoFileLoader externalLogoFileLoader, VaadinLogoImageLoader logoImageLoader)
	{
		this.externalLogoFileLoader = externalLogoFileLoader;
		this.logoImageLoader = logoImageLoader;
	}

	Image getAsResource(IdPVisalSettings configuration, TrustedIdPKey configKey)
	{
		if (configuration.logoURI == null)
			return null;
		return (configuration.federationId == null || configuration.logoURI.startsWith("file:")) ?
			getDirectlyDefinedImage(configuration) :
			getPrefetchedFederationLogo(configuration, configKey);
	}

	private Image getPrefetchedFederationLogo(IdPVisalSettings configuration, TrustedIdPKey configKey)
	{
		try
		{
			return externalLogoFileLoader.getFile(configuration.federationId, configKey, getCurrentRequest().getLocale())
				.map(LogoExposingService::createImage)
				.orElse(null);
		} catch (Exception e)
		{
			log.debug("Can not load logo fetched from URI " + configuration.logoURI, e);
			return null;
		}
	}

	private static Image createImage(File file)
	{
		try 
		{
			@SuppressWarnings("resource")
			FileInputStream byteArrayInputStream = new FileInputStream(file);
			StreamResource streamResource = new StreamResource(file.getName(), () -> byteArrayInputStream);
			Image img = new Image(streamResource, "");
			img.addClassName(LOGO_IMAGE.getName());
			return img;
		} catch (IOException  e)
		{
			log.warn(e);
			return new Image();
		}
	}

	private Image getDirectlyDefinedImage(IdPVisalSettings configuration)
	{
		return logoImageLoader.loadImageFromUri(configuration.logoURI).orElse(null);
	}
}
