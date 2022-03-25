/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import xmlbeans.org.oasis.saml2.assertion.NameIDType;

public class TrustedIdPs
{
	private final Map<TrustedIdPKey, TrustedIdPConfiguration> trustedIdPs;
	private final Map<String, TrustedIdPKey> samlEntityIdToKey;
	
	public TrustedIdPs(Collection<TrustedIdPConfiguration> trustedIdPs)
	{
		this.trustedIdPs = trustedIdPs.stream().collect(Collectors.toMap(idp -> idp.key, idp -> idp));
		this.samlEntityIdToKey = buildEntityToKeyMap(); 
	}
	
	public TrustedIdPs withWebBinding()
	{
		return new TrustedIdPs(trustedIdPs.values().stream()
				.filter(idp -> idp.binding.isWeb())
				.collect(Collectors.toList()));
	}

	private Map<String, TrustedIdPKey> buildEntityToKeyMap()
	{
		return this.trustedIdPs.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getValue().samlId, entry -> entry.getKey()));
	}
	
	public TrustedIdPKey getIdPConfigKey(NameIDType requester)
	{
		return getKeyOfIdP(requester.getStringValue());
	}

	private TrustedIdPKey getKeyOfIdP(String entity)
	{
		return samlEntityIdToKey.get(entity);
	}
	
	public List<PublicKey> getPublicKeysOfIdp(String samlEntityId)
	{
		TrustedIdPKey idpKey = getKeyOfIdP(samlEntityId);
		if (idpKey == null)
			return null;
		return get(idpKey).publicKeys;
	}
	
	public TrustedIdPConfiguration get(TrustedIdPKey key)
	{
		TrustedIdPConfiguration trustedIdPConfiguration = trustedIdPs.get(key);
		if (trustedIdPConfiguration == null)
			throw new IllegalArgumentException("There is no trusted IdP with key " + key);
		return trustedIdPConfiguration;
	}
	
	public Collection<TrustedIdPConfiguration> getAll()
	{
		return Collections.unmodifiableCollection(trustedIdPs.values());
	}
	
	public Set<TrustedIdPKey> getKeys()
	{
		return Collections.unmodifiableSet(trustedIdPs.keySet());
	}

	private boolean isOfSingleFederation(String federationId)
	{
		return trustedIdPs.values().stream()
			.filter(idp -> !federationId.equals(idp.federationId))
			.findAny()
			.map(a -> false)
			.orElse(true);
	}
	
	public TrustedIdPs replaceFederation(TrustedIdPs trustedIdPs, String federationId)
	{
		if (!trustedIdPs.isOfSingleFederation(federationId))
			throw new IllegalArgumentException("Argument must be of a single given federation");
		Set<TrustedIdPConfiguration> currentWithoutFederation = this.trustedIdPs.values().stream()
			.filter(idp -> !federationId.equals(idp.federationId))
			.collect(Collectors.toSet());
		Set<TrustedIdPConfiguration> union = new HashSet<>(currentWithoutFederation);
		union.addAll(trustedIdPs.getAll());
		return new TrustedIdPs(union);
	}

	public TrustedIdPs overrideIdPs(TrustedIdPs trustedIdPs)
	{
		Map<TrustedIdPKey, TrustedIdPConfiguration> copy = new HashMap<>(this.trustedIdPs);
		trustedIdPs.trustedIdPs.values().stream()
			.forEach(idp -> copy.put(idp.key, idp));
		return new TrustedIdPs(copy.values());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(samlEntityIdToKey, trustedIdPs);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrustedIdPs other = (TrustedIdPs) obj;
		return Objects.equals(samlEntityIdToKey, other.samlEntityIdToKey)
				&& Objects.equals(trustedIdPs, other.trustedIdPs);
	}
}
