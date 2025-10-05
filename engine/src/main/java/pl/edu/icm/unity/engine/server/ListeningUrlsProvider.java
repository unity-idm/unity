/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.config.UnityHttpServerConfiguration;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.utils.URLFactory;

@Component
class ListeningUrlsProvider implements AdvertisedAddressProvider
{
	private final URL[] listenUrls;
	private final UnityHttpServerConfiguration serverSettings;
	
	ListeningUrlsProvider(UnityServerConfiguration cfg)
	{
		this.listenUrls = createURLs(cfg.getJettyProperties());
		this.serverSettings = cfg.getJettyProperties();
	}
	
	URL[] getListenUrls()
	{
		return listenUrls;
	}

	@Override
	public URL get()
	{
		String advertisedHost = serverSettings.getValue(UnityHttpServerConfiguration.ADVERTISED_HOST);
		if (advertisedHost == null)
			return getListenUrls()[0];
		
		try 
		{
			return URLFactory.of("https://" + advertisedHost);
		} catch (MalformedURLException e) 
		{
			throw new IllegalStateException("Ups, URL can not be reconstructed, while it should", e);
		}
	}
	
	private static URL[] createURLs(UnityHttpServerConfiguration conf)
	{
		try
		{
			String scheme = conf.getBooleanValue(UnityHttpServerConfiguration.DISABLE_TLS) ? 
					"http" : "https";
			return new URL[] {URLFactory.of(scheme + "://" + conf.getValue(UnityHttpServerConfiguration.HTTP_HOST) +
					":" + conf.getValue(UnityHttpServerConfiguration.HTTP_PORT))};
		} catch (MalformedURLException e)
		{
			throw new ConfigurationException("Can not create server url from host and port parameters: " 
					+ e.getMessage(), e);
		}
	}
}
