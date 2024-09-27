/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;


@Component
class ExistingUserFinder
{
	private final BulkGroupQueryService bulkService;
	private final AttributesHelper attrHelper;
	private final GroupsManagement groupsManagement;
	
	@Autowired
	ExistingUserFinder(@Qualifier("insecure") BulkGroupQueryService bulkService, AttributesHelper attrHelper, @Qualifier("insecure") GroupsManagement groupsManagement)
	{
		this.bulkService = bulkService;
		this.attrHelper = attrHelper;
		this.groupsManagement = groupsManagement;
		
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
	
	Set<EntityWithContactInfo> getEntitiesIdsByContactAddressesWithDirectAttributeCheck(Set<String> contactAddress) throws EngineException
	{
		GroupContents contents = groupsManagement.getContents("/", GroupContents.METADATA);
		AttributeStatement[] attributeStatements = contents.getGroup().getAttributeStatements();
		AttributeType attributeTypeWithSingeltonMetadata = attrHelper.getAttributeTypeWithSingeltonMetadata(ContactEmailMetadataProvider.NAME);
		if (attributeTypeWithSingeltonMetadata == null)
			return getEntitiesIdsByContactAddresses(contactAddress);
		
		
		if (Stream.of(attributeStatements).anyMatch(a -> a.getAssignedAttributeName().equals(attributeTypeWithSingeltonMetadata.getName())))
		{
			return getEntitiesIdsByContactAddresses(contactAddress);
		}
		
		return getEntitiesIdsByContactAddressesOnlyRespectDirectAttributes(contactAddress);
	}
	
	private Set<EntityWithContactInfo> getEntitiesIdsByContactAddressesOnlyRespectDirectAttributes(
			Set<String> contactAddress) throws EngineException
	{
		Set<String> searchedComparableEmails = contactAddress.stream()
				.map(e -> new VerifiableEmail(e).getComparableValue())
				.collect(Collectors.toSet());

		GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
		Map<Long, Entity> entitiesWithoutTargeted = bulkService
				.getGroupEntitiesNoContextWithoutTargeted(bulkMembershipData);

		List<Entity> emailEntities = entitiesWithoutTargeted.values()
				.stream()
				.filter(e -> e.getIdentities()
						.stream()
						.anyMatch(i -> i.getTypeId()
								.equals(EmailIdentity.ID) && searchedComparableEmails.contains(i.getComparableValue())))
				.collect(Collectors.toList());
		Set<EntityWithContactInfo> entitiesWithContactAddress = new HashSet<>();
		Map<Long, Set<String>> entitesGroups = bulkService.getEntitiesGroups(bulkMembershipData);

		for (Entity en : emailEntities)
		{
			Identity emailId = en.getIdentities()
					.stream()
					.filter(id -> id.getTypeId()
							.equals(EmailIdentity.ID))
					.filter(id -> emailsEqual(searchedComparableEmails, id))
					.findAny()
					.orElse(null);
			entitiesWithContactAddress
					.add(new EntityWithContactInfo(en, emailId.getComparableValue(), entitesGroups.get(en.getId())));
		}

		Map<Long, Map<String, AttributeExt>> groupUsersDirectAttributes = bulkService.getGroupUsersDirectAttributes("/",
				bulkMembershipData);

		entitiesWithContactAddress.addAll(searchEntitiesByEmailAttrs(searchedComparableEmails, entitiesWithoutTargeted,
				entitesGroups, groupUsersDirectAttributes));

		return entitiesWithContactAddress;

	}

	Set<EntityWithContactInfo> getEntitiesIdsByContactAddresses(Set<String> contactAddress) throws EngineException
	{
		Set<EntityWithContactInfo> entitiesWithContactAddress = new HashSet<>();
		if (contactAddress == null || contactAddress.isEmpty())
		{
			return entitiesWithContactAddress;
		}
		
		GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
		Map<Long, EntityInGroupData> members = bulkService.getMembershipInfo(bulkMembershipData);

		Set<String> searchedComparableEmails = contactAddress.stream().map(e -> new VerifiableEmail(e).getComparableValue()).collect(Collectors.toSet());
		
		for (EntityInGroupData info : members.values())
		{
			Identity emailId = info.entity.getIdentities().stream()
					.filter(id -> id.getTypeId().equals(EmailIdentity.ID))
					.filter(id -> emailsEqual(searchedComparableEmails, id))
					.findAny().orElse(null);
			if (emailId != null)
				entitiesWithContactAddress.add(new EntityWithContactInfo(info.entity, emailId.getComparableValue(), info.groups));
		}

		Set<EntityWithContactInfo> entitiesByEmailAttr = searchEntitiesByEmailAttrs(members, searchedComparableEmails);
		entitiesWithContactAddress.addAll(entitiesByEmailAttr);
		return entitiesWithContactAddress;
		
	}

	private boolean emailsEqual(String comparableEmail1, Identity emailIdentity)
	{
		VerifiableEmail verifiableEmail = EmailIdentity.fromIdentityParam(emailIdentity);
		return comparableEmail1.equals(verifiableEmail.getComparableValue());
	}
	
	private boolean emailsEqual(Set<String> comparableEmails, Identity emailIdentity)
	{
		VerifiableEmail verifiableEmail = EmailIdentity.fromIdentityParam(emailIdentity);
		return comparableEmails.contains(verifiableEmail.getComparableValue());
	}
	
	private Set<EntityWithContactInfo> searchEntitiesByEmailAttrs(Set<String> searchedComparableEmails, Map<Long, Entity> entitiesWithoutTargeted,
			Map<Long, Set<String>> entitesGroups, Map<Long, Map<String, AttributeExt>> groupUsersDirectAttributes) throws EngineException
	{
		
		Set<EntityWithContactInfo> entitiesWithContactAddressAttr = new HashSet<>();
		for (Entry<Long, Map<String, AttributeExt>> en : groupUsersDirectAttributes.entrySet())
		{
			VerifiableElementBase contactEmail = attrHelper
					.getFirstVerifiableAttributeValueFilteredByMeta(ContactEmailMetadataProvider.NAME, en.getValue()
							.values()
							.stream()
							.map(e -> (Attribute) e)
							.collect(Collectors.toList()))
					.orElse(null);
			if (contactEmail != null && contactEmail.getValue() != null)
			{
				VerifiableEmail verifiableEmail = (VerifiableEmail) contactEmail;
				if (searchedComparableEmails.contains(verifiableEmail.getComparableValue()))
					entitiesWithContactAddressAttr.add(new EntityWithContactInfo(
							entitiesWithoutTargeted.get(en.getKey()),
							verifiableEmail.getComparableValue(), entitesGroups.get(en.getKey())));
			}
		}
		return entitiesWithContactAddressAttr;
	}
	
	private Set<EntityWithContactInfo> searchEntitiesByEmailAttrs(Map<Long, EntityInGroupData> membersWithGroups, Set<String> comparableContactAddresses)
			throws EngineException
	{
		Set<EntityWithContactInfo> entitiesWithContactAddressAttr = new HashSet<>();
		for (EntityInGroupData info : membersWithGroups.values())
		{
			VerifiableElementBase contactEmail = attrHelper.getFirstVerifiableAttributeValueFilteredByMeta(ContactEmailMetadataProvider.NAME,
					info.groupAttributesByName.values().stream().map(e -> (Attribute) e)
							.collect(Collectors.toList())).orElse(null);
			if (contactEmail != null && contactEmail.getValue() != null)
			{
				VerifiableEmail verifiableEmail = (VerifiableEmail)contactEmail;
				if (comparableContactAddresses.contains(verifiableEmail.getComparableValue()))
					entitiesWithContactAddressAttr.add(new EntityWithContactInfo(info.entity, verifiableEmail.getComparableValue(), info.groups));
			}
		}

		return entitiesWithContactAddressAttr;
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
