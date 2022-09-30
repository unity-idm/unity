/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import com.google.common.collect.Sets;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

import java.util.*;
import java.util.stream.Collectors;

public class TrustedIdPs
{
	private final Map<TrustedIdPKey, TrustedIdPConfiguration> trustedIdPs;
	private final Map<String, Set<TrustedIdPConfiguration>> samlEntityIdToKey;
	
	public TrustedIdPs(Collection<TrustedIdPConfiguration> trustedIdPs)
	{
		List<String> duplicates = trustedIdPs.stream()
				.collect(Collectors.groupingBy(trustedIdPConfiguration -> trustedIdPConfiguration.key))
				.values().stream()
				.filter(trustedIdPConfigurations -> trustedIdPConfigurations.size() > 1)
				.map(trustedIdPConfigurations ->
						String.format(
								"Duplicated samlId: %s for federations %s",
								trustedIdPConfigurations.get(0).samlId,
								trustedIdPConfigurations.stream().map(conf -> conf.federationId).collect(Collectors.toList()))
				)
				.collect(Collectors.toList());
		if(duplicates.size() > 0)
			throw new IllegalArgumentException(String.join(";", duplicates));
		this.trustedIdPs = trustedIdPs.stream()
				.collect(Collectors.toUnmodifiableMap(idp -> idp.key, idp -> idp));
		this.samlEntityIdToKey = buildEntityToKeyMap(); 
	}
	
	public TrustedIdPs withWebBinding()
	{
		return new TrustedIdPs(trustedIdPs.values().stream()
				.filter(idp -> EndpointBindingCategory.WEB.matches(idp.binding))
				.collect(Collectors.toList()));
	}

	private Map<String, Set<TrustedIdPConfiguration>> buildEntityToKeyMap()
	{
		return this.trustedIdPs.values().stream()
				.collect(Collectors.toUnmodifiableMap(entry -> entry.samlId, 
						entry -> Set.of(entry),
						Sets::union));
	}
	
	public Optional<TrustedIdPConfiguration> getIdPBySamlRequester(NameIDType requester, EndpointBindingCategory bindingCategory)
	{
		return getIdPBySamlEntityId(requester.getStringValue(), bindingCategory);
	}

	private Optional<TrustedIdPConfiguration> getIdPBySamlEntityId(String entity, EndpointBindingCategory bindingCategory)
	{
		Set<TrustedIdPConfiguration> idps = samlEntityIdToKey.get(entity);
		if (idps == null)
			return Optional.empty();
		return idps.stream()
			.filter(idp -> bindingCategory.matches(idp.binding))
			.findFirst();
	}
	
	public TrustedIdPConfiguration get(TrustedIdPKey key)
	{
		TrustedIdPConfiguration trustedIdPConfiguration = trustedIdPs.get(key);
		if (trustedIdPConfiguration == null)
			throw new IllegalArgumentException("There is no trusted IdP with key " + key);
		return trustedIdPConfiguration;
	}
	
	public boolean contains(TrustedIdPKey key)
	{
		return trustedIdPs.containsKey(key);
	}
	
	public Collection<TrustedIdPConfiguration> getAll()
	{
		return Collections.unmodifiableCollection(trustedIdPs.values());
	}
	
	public Set<TrustedIdPKey> getKeys()
	{
		return Collections.unmodifiableSet(trustedIdPs.keySet());
	}

	public Set<Map.Entry<TrustedIdPKey, TrustedIdPConfiguration>> getEntrySet()
	{
		return Collections.unmodifiableSet(trustedIdPs.entrySet());
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
	
	public enum EndpointBindingCategory
	{
		WEB(Binding.HTTP_POST, Binding.HTTP_REDIRECT), 
		SOAP(Binding.SOAP);

		private final Set<Binding> matchingBindings;
		
		private EndpointBindingCategory(Binding... matchingBindings)
		{
			this.matchingBindings = Set.of(matchingBindings);
		}

		boolean matches(Binding binding)
		{
			return matchingBindings.contains(binding);
		}
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
