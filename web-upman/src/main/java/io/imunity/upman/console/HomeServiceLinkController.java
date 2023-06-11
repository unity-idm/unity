/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console;

import io.imunity.upman.UpmanEndpointProperties;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.home.HomeEndpointConstants;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HomeServiceLinkController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, HomeServiceLinkController.class);

	private EndpointManagement endpointMan;
	private AdvertisedAddressProvider advertisedAddrProvider;

	@Autowired
	public HomeServiceLinkController(@Qualifier("insecure") EndpointManagement endpointMan,
			AdvertisedAddressProvider advertisedAddrProvider)
	{

		this.endpointMan = endpointMan;
		this.advertisedAddrProvider = advertisedAddrProvider;
	}

	public Optional<String> getHomeLinkIfAvailable(UpmanEndpointProperties config)
	{

		if (!config.isHomeIsEnabled())
			return Optional.empty();

		Set<ResolvedEndpoint> allEndpoints = getAllHomeEndpoints();
		if (allEndpoints.isEmpty())
		{
			log.debug("Home endpoint link is enabled, but home endpoins are not available");
			return Optional.empty();

		}

		String homeEndpointName = config.getHomeEndpoint();

		if (homeEndpointName == null)
		{
			log.debug("Home endpoint link is enabled, using first available home endpoint");
			return Optional.ofNullable(getLinkToHomeEndpoint(allEndpoints.iterator().next()));
		} else
		{

			ResolvedEndpoint endpoint = allEndpoints.stream()
					.filter(e -> e.getName().equals(config.getHomeEndpoint())).findAny()
					.orElse(null);

			if (endpoint == null)
			{
				log.warn("Home endpoint link is enabled, but endpoint with name " + homeEndpointName
						+ " is not available");
				return Optional.empty();
			}

			return Optional.ofNullable(getLinkToHomeEndpoint(endpoint));
		}
	}

	private String getLinkToHomeEndpoint(ResolvedEndpoint endpoint)
	{
		if (endpoint == null)
			return null;
		return advertisedAddrProvider.get() + endpoint.getEndpoint().getContextAddress();
	}

	public Set<ResolvedEndpoint> getAllHomeEndpoints()
	{
		try
		{
			return endpointMan.getDeployedEndpoints().stream()
					.filter(e -> e.getType().getName().equals(HomeEndpointConstants.ENDPOINT_NAME))
					.collect(Collectors.toSet());
		} catch (EngineException e)
		{
			log.error("Can not get home endpoints", e);
			return Collections.emptySet();
		}

	}
}
