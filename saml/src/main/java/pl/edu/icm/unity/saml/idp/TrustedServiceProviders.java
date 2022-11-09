/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TrustedServiceProviders
{
	private final Map<String, TrustedServiceProviderConfiguration> entityIdToSPConfiguration;

	public TrustedServiceProviders(Collection<TrustedServiceProviderConfiguration> trustedSPs)
	{
		this.entityIdToSPConfiguration = trustedSPs.stream()
				.collect(Collectors.toMap(sp -> sp.entityId, sp -> sp));
	}

	public TrustedServiceProviderConfiguration getSPConfig(String entityId)
	{
		return entityIdToSPConfiguration.get(entityId);
	}

	public Set<TrustedServiceProviderConfiguration> getSPConfigs()
	{
		return Set.copyOf(entityIdToSPConfiguration.values());
	}

	public void replace(Set<TrustedServiceProviderConfiguration> trustedIdPs)
	{
		trustedIdPs.forEach(config -> entityIdToSPConfiguration.put(config.entityId, config));
	}
}
