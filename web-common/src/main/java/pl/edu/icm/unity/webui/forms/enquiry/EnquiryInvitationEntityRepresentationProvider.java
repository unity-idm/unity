/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

class EnquiryInvitationEntityRepresentationProvider
{
	private final Function<Long, Optional<String>> entityDisplayedNameProvider;

	EnquiryInvitationEntityRepresentationProvider(Function<Long, Optional<String>> displayedNameProvider)
	{
		this.entityDisplayedNameProvider = displayedNameProvider;
	}

	String getEntityRepresentation(Entity entity)
	{

		StringBuilder entityRep = new StringBuilder();
		Optional<String> displayedName = entityDisplayedNameProvider.apply(entity.getId());

		if (displayedName.isPresent())
		{
			entityRep.append(displayedName.get());
		}

		List<Identity> remoteIds = getRemoteIdentities(entity);
		if (!remoteIds.isEmpty())
		{
			if (displayedName.isPresent())
				entityRep.append(": ");
			entityRep.append(
					remoteIds.stream().map(i -> getRemoteIdentityRepresentation(i)).collect(Collectors.joining(" & ")));

		} else
		{
			String localIds = getLocalIdentitiesWithoutAnonymous(entity).stream()
					.map(i -> getLocalIdentityRepresentation(i)).collect(Collectors.joining(" & "));
			if (!localIds.isEmpty() && displayedName.isPresent())
				entityRep.append(": ");
			entityRep.append(localIds);
		}

		return entityRep.toString();
	}

	private String getLocalIdentityRepresentation(Identity identity)
	{
		return identity.getValue();
	}

	private String getRemoteIdentityRepresentation(Identity identity)
	{
		try
		{
			URL url = new URL(identity.getRemoteIdp());
			return url.getHost();
		} catch (MalformedURLException e)
		{
			return identity.getRemoteIdp();
		}
	}

	private List<Identity> getRemoteIdentities(Entity entity)
	{
		return entity.getIdentities().stream().filter(i -> !i.isLocal()).collect(Collectors.toList());
	}

	private List<Identity> getLocalIdentitiesWithoutAnonymous(Entity entity)
	{
		return entity.getIdentities().stream().filter(
				i -> i.isLocal() && ResolvedInvitationParam.NOT_ANONYMOUS_IDENTITIES_TYPES.contains(i.getTypeId()))
				.collect(Collectors.toList());
	}
}
