/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.components;

import io.imunity.upman.UpmanEndpointProperties;
import io.imunity.upman.av23.Vaadin23WebAppContext;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.home.HomeEndpointConstants;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HomeServiceLinkService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, HomeServiceLinkService.class);

	private final EndpointManagement endpointMan;
	private final AdvertisedAddressProvider advertisedAddrProvider;

	public HomeServiceLinkService(@Qualifier("insecure") EndpointManagement endpointMan,
	                              AdvertisedAddressProvider advertisedAddrProvider)
	{

		this.endpointMan = endpointMan;
		this.advertisedAddrProvider = advertisedAddrProvider;
	}

	public Optional<String> getHomeLinkIfAvailable()
	{
		UpmanEndpointProperties upmanEndpointProperties = new UpmanEndpointProperties(Vaadin23WebAppContext.getCurrentWebAppContextProperties());

		if (!upmanEndpointProperties.isHomeIsEnabled())
			return Optional.empty();

		Set<ResolvedEndpoint> allEndpoints = getAllHomeEndpoints();
		if (allEndpoints.isEmpty())
		{
			log.debug("Home endpoint link is enabled, but home endpoins are not available");
			return Optional.empty();

		}

		String homeEndpointName = upmanEndpointProperties.getHomeEndpoint();

		if (homeEndpointName == null)
		{
			log.debug("Home endpoint link is enabled, using first available home endpoint");
			return getLinkToHomeEndpoint(allEndpoints.iterator().next());
		} else
		{

			ResolvedEndpoint endpoint = allEndpoints.stream()
					.filter(e -> e.getName().equals(upmanEndpointProperties.getHomeEndpoint())).findAny()
					.orElse(null);

			if (endpoint == null)
			{
				log.warn("Home endpoint link is enabled, but endpoint with name " + homeEndpointName
						+ " is not available");
				return Optional.empty();
			}

			return getLinkToHomeEndpoint(endpoint);
		}
	}

	private Optional<String> getLinkToHomeEndpoint(ResolvedEndpoint endpoint)
	{
		return Optional.ofNullable(endpoint)
				.map(value -> advertisedAddrProvider.get() + value.getEndpoint().getContextAddress());
	}

	private Set<ResolvedEndpoint> getAllHomeEndpoints()
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
