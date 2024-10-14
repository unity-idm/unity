/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;


@Component
class ExistingUserFinder
{
	private final BulkGroupQueryService bulkService;
	private final AttributesHelper attrHelper;
	private final GroupsManagement groupsManagement;
	private final IdentityDAO identityDAO;
	private final AttributeDAO attributeDAO;
	private final EntityDAO entityDAO;
	private final EntityResolver idResolver;
	private final EntityCredentialsHelper credentialsHelper;
	private final IdentityHelper identityHelper;
	private final MembershipDAO membershipDAO;

	@Autowired
	ExistingUserFinder(@Qualifier("insecure") BulkGroupQueryService bulkService, AttributesHelper attrHelper, @Qualifier("insecure") GroupsManagement groupsManagement, 
			IdentityDAO identityDAO, AttributeDAO attributeDAO, EntityDAO entityDAO, EntityResolver idResolver, EntityCredentialsHelper credentialsHelper, IdentityHelper identityHelper,
			MembershipDAO membershipDAO)
	{
		this.bulkService = bulkService;
		this.attrHelper = attrHelper;
		this.groupsManagement = groupsManagement;
		this.attributeDAO = attributeDAO;
		this.identityDAO = identityDAO;
		this.entityDAO = entityDAO;
		this.idResolver = idResolver;
		this.credentialsHelper = credentialsHelper;
		this.identityHelper = identityHelper;
		this.membershipDAO = membershipDAO;
		
	}

	Set<Entity> getEntitiesIdsByContactAddress(String contactAddress) throws EngineException
	{
		if (contactAddress == null || contactAddress.isEmpty())
		{
			return Collections.emptySet();
		}
		
		Set<Entity> entitiesWithContactAddress = new HashSet<>();
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
		if (contactAddress == null || contactAddress.isEmpty())
		{
			return Collections.emptySet();
		}
	
		AttributeType attributeTypeWithSingeltonMetadata = attrHelper.getAttributeTypeWithSingeltonMetadata(ContactEmailMetadataProvider.NAME);
		
		Set<String> searchedComparableEmails = contactAddress.stream()
			.map(e -> new VerifiableEmail(e).getComparableValue())
			.collect(Collectors.toSet());
		
		if (attributeTypeWithSingeltonMetadata == null)
			return getEntitiesIdsByContactAddressesInIdentity(searchedComparableEmails);
	
		GroupContents contents = groupsManagement.getContents("/", GroupContents.METADATA);
		AttributeStatement[] attributeStatements = contents.getGroup().getAttributeStatements();
		if (Stream.of(attributeStatements).anyMatch(a -> a.getAssignedAttributeName().equals(attributeTypeWithSingeltonMetadata.getName())))
		{
			return getEntitiesIdsByContactAddresses(searchedComparableEmails);
		}
		
		return getEntitiesIdsByContactAddressesOnlyRespectDirectAttributes(searchedComparableEmails, attributeTypeWithSingeltonMetadata);
	}
	
	@Transactional
	private Set<EntityWithContactInfo> getEntitiesIdsByContactAddressesOnlyRespectDirectAttributes(
			Set<String> searchedComparableEmails, AttributeType attributeTypeWithSingeltonMetadata) throws EngineException
	{
		Set<EntityWithContactInfo> entitiesWithContactAddress = getEntitiesIdsByContactAddressesInIdentity(searchedComparableEmails);
		List<StoredAttribute> attributesOfGroupMembers = attributeDAO.getAttributesOfGroupMembers(List.of(attributeTypeWithSingeltonMetadata.getName()), List.of("/"));
	
		Map<Long, String> searchedEntitiesByEmailAttrs = searchEntitiesByEmailAttrs(searchedComparableEmails, attributesOfGroupMembers);
		for (Long entityId : searchedEntitiesByEmailAttrs.keySet())
		{
			EntityParam entityParam =  new EntityParam(entityId);
			Entity entity = getEntity(entityParam);
			Set<String> groups = getGroups(entity.getId());
			entitiesWithContactAddress.add(new EntityWithContactInfo(entity, searchedEntitiesByEmailAttrs.get(entityId), groups));
		}
		
		return entitiesWithContactAddress;
	}
	@Transactional
	private Set<EntityWithContactInfo> getEntitiesIdsByContactAddressesInIdentity(
			Set<String> searchedComparableEmails) throws EngineException
	{
		Set<EntityWithContactInfo> entitiesWithContactAddress = new HashSet<>();
	
		Set<Long> idByTypeAndValues = identityDAO.getIdByTypeAndValues(EmailIdentity.ID, searchedComparableEmails.stream().toList());	
		
		for (Long entityId : idByTypeAndValues)
		{
			entitiesWithContactAddress.add(resolveToEntityWithContactAddress(new EntityParam(entityId), searchedComparableEmails));
		}
		return entitiesWithContactAddress;
		
		
	}
	
	private EntityWithContactInfo resolveToEntityWithContactAddress(EntityParam entityParam, Set<String> searchedComparableEmails ) throws EngineException
	{
		Entity entity = getEntity(entityParam);
		Set<String> groups = getGroups(entity.getId());
		Identity emailId = entity.getIdentities().stream()
				.filter(id -> id.getTypeId().equals(EmailIdentity.ID))
				.filter(id -> emailsEqual(searchedComparableEmails, id))
				.findAny().orElse(null);
		return new EntityWithContactInfo(entity, emailId.getComparableValue(), groups);
	}
	
	private Map<Long, String> searchEntitiesByEmailAttrs(Set<String> searchedComparableEmails, List<StoredAttribute> attributes) throws EngineException
	{
		Map<Long, String> entities = new HashMap<>();
		for (StoredAttribute attr: attributes)
		{
			VerifiableElementBase contactEmail = attrHelper
					.getFirstVerifiableAttributeValueFilteredByMeta(ContactEmailMetadataProvider.NAME, List.of(attr).stream().map(e -> (Attribute) e.getAttribute()).collect(Collectors.toList()))
					.orElse(null);
			if (contactEmail != null && contactEmail.getValue() != null)
			{
				VerifiableEmail verifiableEmail = (VerifiableEmail) contactEmail;
				if (searchedComparableEmails.contains(verifiableEmail.getComparableValue()))
				{
					entities.put(attr.getEntityId(), verifiableEmail.getValue());
				}
			}
		}
		return entities;
	}
	
	private Set<EntityWithContactInfo> getEntitiesIdsByContactAddresses(Set<String> searchedComparableEmails) throws EngineException
	{
		Set<EntityWithContactInfo> entitiesWithContactAddress = new HashSet<>();	
		GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
		Map<Long, EntityInGroupData> members = bulkService.getMembershipInfo(bulkMembershipData);

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

	private Entity getEntity(EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		CredentialInfo credInfo = credentialsHelper.getCredentialInfo(entityId);
		EntityInformation theState = entityDAO.getByKey(entityId);
		return new Entity(getIdentitiesForEntity(entityId), theState, credInfo);
	}

	private List<Identity> getIdentitiesForEntity(long entityId) throws IllegalIdentityValueException
	{
		List<Identity> ret = identityHelper.getIdentitiesForEntity(entityId, null);
		identityHelper.addDynamic(entityId, ret.stream()
				.map(Identity::getTypeId)
				.collect(Collectors.toSet()), ret, null);
		return ret;
	}

	private Set<String> getGroups(long entityId)
	{
		return membershipDAO.getEntityMembership(entityId)
				.stream()
				.map(g -> g.getGroup())
				.collect(Collectors.toSet());
	}
}
