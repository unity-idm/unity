/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.enquiry;

import io.imunity.vaadin.endpoint.common.forms.ResolvedInvitationParam;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

class EnquiryInvitationEntityRepresentationProvider
{
	private final Function<Long, Optional<String>> entityDisplayedNameProvider;
	private final MessageSource msg;

	EnquiryInvitationEntityRepresentationProvider(Function<Long, Optional<String>> displayedNameProvider, MessageSource msg)
	{
		this.entityDisplayedNameProvider = displayedNameProvider;
		this.msg = msg;
	}

	public String getEntityRepresentation(Entity entity)
	{

		StringBuilder entityRep = new StringBuilder();
		Optional<String> displayedName = entityDisplayedNameProvider.apply(entity.getId());

		displayedName.ifPresent(entityRep::append);

		List<Identity> remoteIds = getRemoteIdentities(entity);
		if (!remoteIds.isEmpty())
		{
			if (displayedName.isPresent())
				entityRep.append(": ");
			entityRep.append(msg.getMessage("EnquiryInvitationEntityRepresentationProvider.linkedWith") + " ");
			entityRep.append(
					remoteIds.stream().map(this::getRemoteIdentityRepresentation).collect(Collectors.joining(" & ")));

		} else
		{
			String localIds = getLocalIdentitiesWithoutAnonymous(entity).stream()
					.map(this::getLocalIdentityRepresentation).collect(Collectors.joining(" & "));
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
			String host = url.getHost();
			return host == null || host.isEmpty() ? identity.getRemoteIdp() : host;
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
