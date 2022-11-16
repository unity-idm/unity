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
	private final Map<SamlEntityId, TrustedServiceProvider> entityIdToSPConfiguration;

	public TrustedServiceProviders(Collection<TrustedServiceProvider> trustedSPs)
	{
		this.entityIdToSPConfiguration = trustedSPs.stream()
				.collect(Collectors.toMap(sp -> sp.entityId, sp -> sp));
	}

	public TrustedServiceProvider getSPConfig(SamlEntityId entityId)
	{
		return entityIdToSPConfiguration.get(entityId);
	}

	public Set<TrustedServiceProvider> getSPConfigs()
	{
		return Set.copyOf(entityIdToSPConfiguration.values());
	}

	public void replace(Set<TrustedServiceProvider> trustedIdPs)
	{
		trustedIdPs.forEach(config -> entityIdToSPConfiguration.put(config.entityId, config));
	}
}
