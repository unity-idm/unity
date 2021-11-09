/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;
import pl.edu.icm.unity.types.basic.VerifiableEmail;


@Component
class ExistingUserFinder
{
	private final BulkGroupQueryService bulkService;
	private final AttributesHelper attrHelper;
	
	@Autowired
	ExistingUserFinder(@Qualifier("insecure") BulkGroupQueryService bulkService, AttributesHelper attrHelper)
	{
		this.bulkService = bulkService;
		this.attrHelper = attrHelper;
	}

	Set<Entity> getEntitiesIdsByContactAddress(String contactAddress) throws EngineException
	{
		Set<Entity> entitiesWithContactAddress = new HashSet<>();
		if (contactAddress == null || contactAddress.isEmpty())
		{
			return entitiesWithContactAddress;
		}
		
		GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
		Map<Long, EntityInGroupData> members = bulkService.getMembershipInfo(bulkMembershipData);

		VerifiableEmail searchedEmail = new VerifiableEmail(contactAddress);
		String searchedComparable = searchedEmail.getComparableValue();
		
		for (EntityInGroupData info : members.values())
		{
			Identity emailId = info.entity.getIdentities().stream()
					.filter(id -> id.getTypeId().equals(EmailIdentity.ID))
					.filter(id -> emailsEqual(searchedComparable, id))
					.findAny().orElse(null);
			if (emailId != null)
				entitiesWithContactAddress.add(info.entity);
		}

		Set<Entity> entitiesByEmailAttr = searchEntitiesByEmailAttr(members, searchedComparable);
		entitiesWithContactAddress.addAll(entitiesByEmailAttr);
		return entitiesWithContactAddress;
		
	}

	private boolean emailsEqual(String comparableEmail1, Identity emailIdentity)
	{
		VerifiableEmail verifiableEmail = EmailIdentity.fromIdentityParam(emailIdentity);
		return comparableEmail1.equals(verifiableEmail.getComparableValue());
	}
	
	private Set<Entity> searchEntitiesByEmailAttr(Map<Long, EntityInGroupData> membersWithGroups, String comparableContactAddress)
			throws EngineException
	{
		Set<Entity> entitiesWithContactAddressAttr = new HashSet<>();
		for (EntityInGroupData info : membersWithGroups.values())
		{
			VerifiableElementBase contactEmail = attrHelper.getFirstVerifiableAttributeValueFilteredByMeta(ContactEmailMetadataProvider.NAME,
					info.groupAttributesByName.values().stream().map(e -> (Attribute) e)
							.collect(Collectors.toList())).orElse(null);
			if (contactEmail != null && contactEmail.getValue() != null)
			{
				VerifiableEmail verifiableEmail = (VerifiableEmail)contactEmail;
				if (verifiableEmail.getComparableValue().equals(comparableContactAddress))
					entitiesWithContactAddressAttr.add(info.entity);
			}
		}

		return entitiesWithContactAddressAttr;
	}	
}
