/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.io.File;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.DownloadHandler;

import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.logo.CachedLogoFileLoader;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;
import pl.edu.icm.unity.oauth.client.federation.OAuthFederationLogoDownloader;

import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE;

/**
 * Resolves the logo to show for an OAuth provider. For providers coming from a federation, the
 * {@code iconUrl} is a self-declared, untrusted value of the remote entity, so it is never fetched
 * directly: only the locally cached copy prefetched out-of-band by {@link OAuthFederationLogoDownloader}
 * is served, and no logo is shown on a cache miss. Providers that aren't federation sourced, or whose
 * icon uses a {@code file:} URI, are admin-configured/trusted and are fetched directly as before.
 * Mirrors SAML's {@code LogoExposingService}.
 */
@Component
public class OAuthProviderLogoLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthProviderLogoLoader.class);

	private final CachedLogoFileLoader cachedLogoFileLoader;
	private final VaadinLogoImageLoader vaadinLogoImageLoader;

	public OAuthProviderLogoLoader(CachedLogoFileLoader cachedLogoFileLoader, VaadinLogoImageLoader vaadinLogoImageLoader)
	{
		this.cachedLogoFileLoader = cachedLogoFileLoader;
		this.vaadinLogoImageLoader = vaadinLogoImageLoader;
	}

	public Optional<Image> loadLogo(OAuthProviderConfiguration provider, MessageSource msg)
	{
		String iconUrl = provider.iconUrl() != null ? provider.iconUrl().getValue(msg) : null;
		if (iconUrl == null || iconUrl.isEmpty())
			return Optional.empty();
		return (provider.federationId() == null || iconUrl.startsWith("file:")) ?
				vaadinLogoImageLoader.loadImageFromUri(iconUrl).map(Image.class::cast) :
				getPrefetchedFederationLogo(provider);
	}

	private Optional<Image> getPrefetchedFederationLogo(OAuthProviderConfiguration provider)
	{
		try
		{
			return cachedLogoFileLoader.getFile(OAuthFederationLogoDownloader.CACHE_GROUP, provider.federationId(),
						provider.key(), getSafeLocale())
					.map(OAuthProviderLogoLoader::createImage);
		} catch (Exception e)
		{
			log.debug("Can not load cached logo of federation provider " + provider.key(), e);
			return Optional.empty();
		}
	}

	private static Locale getSafeLocale()
	{
		var req = VaadinService.getCurrentRequest();
		return req != null ? req.getLocale() : Locale.getDefault();
	}

	private static Image createImage(File file)
	{
		Image img = new Image(DownloadHandler.forFile(file), "");
		img.addClassName(LOGO_IMAGE.getName());
		return img;
	}
}
