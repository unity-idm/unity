/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;

public class ExistingUserFinderTest
{
	private static final EmailIdentity emailIdType = new EmailIdentity();
	private BulkGroupQueryService bulkService = mock(BulkGroupQueryService.class);
	private AttributesHelper attrHelper = mock(AttributesHelper.class);
	private GroupsManagement groupsManagement = mock(GroupsManagement.class);
	private EntityManagement entityManagement = mock(EntityManagement.class);
	private IdentityDAO identityDAO = mock(IdentityDAO.class);
	private AttributeDAO attributeDAO = mock(AttributeDAO.class);
	
	
	
	@Test
	public void shouldFindByIdentityCaseInsensitive() throws EngineException
	{
		EntityInGroupData entityData = new EntityInGroupData(createEmailEntity("addr1@EXample.com", 13), null, null,
				null, null, null);
		when(bulkService.getMembershipInfo(any())).thenReturn(ImmutableMap.of(13l, entityData));
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement, entityManagement, identityDAO, attributeDAO);

		Set<Entity> entityIdByContactAddress = userFinder.getEntitiesIdsByContactAddress("Addr1@examplE.com");

		assertThat(entityIdByContactAddress.iterator().next().getId()).isEqualTo(13);
	}

	@Test
	public void shouldFindByAttributeCaseInsensitive() throws EngineException
	{
		AttributeExt emailAttr = new AttributeExt(VerifiableEmailAttribute.of("email", "/", "addr1@EXample.com"), true);
		EntityInGroupData entityData = new EntityInGroupData(createEmailEntity("other@example.com", 13), null, null,
				ImmutableMap.of("email", emailAttr), null, null);
		when(bulkService.getMembershipInfo(any())).thenReturn(ImmutableMap.of(13l, entityData));
		when(attrHelper.getFirstVerifiableAttributeValueFilteredByMeta(eq(ContactEmailMetadataProvider.NAME), any()))
				.thenReturn(Optional.of(VerifiableEmail.fromJsonString(emailAttr.getValues().get(0))));
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement, entityManagement, identityDAO, attributeDAO);

		Set<Entity> entityIdByContactAddress = userFinder.getEntitiesIdsByContactAddress("Addr1@examplE.com");

		assertThat(entityIdByContactAddress.iterator().next().getId()).isEqualTo(13);
	}

	@Test
	public void shouldFindAllEntitiesWithGivenEmail() throws EngineException
	{
		AttributeExt emailAttr = new AttributeExt(VerifiableEmailAttribute.of("email", "/", "addr1@EXample.com"), true);

		EntityInGroupData entityWithEmailAttrData = new EntityInGroupData(createEmailEntity("other@example.com", 13),
				null, null, ImmutableMap.of("email", emailAttr), null, null);
		EntityInGroupData entityWithIdEmailData = new EntityInGroupData(createEmailEntity("addr1@EXample.com", 14),
				null, null, new HashMap<>(), null, null);

		when(bulkService.getMembershipInfo(any()))
				.thenReturn(ImmutableMap.of(13l, entityWithEmailAttrData, 14l, entityWithIdEmailData));
		when(attrHelper.getFirstVerifiableAttributeValueFilteredByMeta(eq(ContactEmailMetadataProvider.NAME),
				eq(Arrays.asList(emailAttr))))
						.thenReturn(Optional.of(VerifiableEmail.fromJsonString(emailAttr.getValues().get(0))));

		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement, entityManagement, identityDAO, attributeDAO);
		Set<Entity> entityIdByContactAddress = userFinder.getEntitiesIdsByContactAddress("Addr1@examplE.com");
		assertThat(entityIdByContactAddress.size()).isEqualTo(2);
		assertThat(entityIdByContactAddress.stream().map(e -> e.getId()).collect(Collectors.toSet())).contains(14L, 13L);
	}
	
	
	@Test
	public void shouldFindAllEntitiesWithGivenEmailRespectOnlyDirectAttributes() throws EngineException
	{	
		AttributeExt emailAttr = new AttributeExt(VerifiableEmailAttribute.of(ContactEmailMetadataProvider.NAME, "/", "addr1@EXample.com"), true);
		Entity entityWithEmailAttrData = createEmailEntity("other@example.com", 13);
		Entity entityWithIdEmailData = createEmailEntity("addr1@EXample.com", 14);
		
		
		
		when(attrHelper.getFirstVerifiableAttributeValueFilteredByMeta(eq(ContactEmailMetadataProvider.NAME),
				eq(Arrays.asList(emailAttr))))
						.thenReturn(Optional.of(VerifiableEmail.fromJsonString(emailAttr.getValues().get(0))));
		GroupContents contents = new GroupContents();
		Group group = new Group("/");
		AttributeStatement statement = new AttributeStatement("true", "/", ConflictResolution.merge, "name", "'name'");
		group.setAttributeStatements(new AttributeStatement[] {statement});
		contents.setGroup(group);	
		when(groupsManagement.getContents("/", GroupContents.METADATA)).thenReturn(contents);
		when(attrHelper.getAttributeTypeWithSingeltonMetadata(ContactEmailMetadataProvider.NAME)).thenReturn(new AttributeType(ContactEmailMetadataProvider.NAME, "email"));
		
		when(identityDAO.getIdByTypeAndValues(EmailIdentity.ID, List.of("addr1@example.com"))).thenReturn(Set.of(14L));
		when(attributeDAO.getAttributesOfGroupMembers(List.of(ContactEmailMetadataProvider.NAME), List.of("/"))).thenReturn(List.of(new StoredAttribute(emailAttr, 13L)));
		when(entityManagement.getEntity(new EntityParam(13L))).thenReturn(entityWithEmailAttrData);
		when(entityManagement.getEntity(new EntityParam(14L))).thenReturn(entityWithIdEmailData);
		when(entityManagement.getGroups(new EntityParam(14L))).thenReturn(Map.of("/", new GroupMembership("/", 14L, new Date()), "/B", new GroupMembership("/B", 14L, new Date())));
		when(entityManagement.getGroups(new EntityParam(13L))).thenReturn(Map.of("/", new GroupMembership("/", 13L, new Date()), "/A", new GroupMembership("/A", 14L, new Date())));
	
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement, entityManagement, identityDAO, attributeDAO);
		Set<EntityWithContactInfo> entityIdByContactAddress = userFinder.getEntitiesIdsByContactAddressesWithDirectAttributeCheck(Set.of("Addr1@examplE.com"));
		assertThat(entityIdByContactAddress.stream().map(e -> e.entity.getId()).collect(Collectors.toSet())).contains(14L, 13L);	
		assertThat(entityIdByContactAddress.stream().filter(e -> e.entity.getId().equals(13l)).findFirst().get().groups).containsExactlyElementsOf(Set.of("/A", "/"));	
		assertThat(entityIdByContactAddress.stream().filter(e -> e.entity.getId().equals(14l)).findFirst().get().groups).containsExactlyElementsOf(Set.of("/B", "/"));	
	}
	
	@Test
	public void shouldFindAllEntitiesWithGivenEmailRespectOnlyEmailIdentity() throws EngineException
	{
		Entity entityWithIdEmailData1 = createEmailEntity("addr1@EXample.com", 14);
		Entity entityWithIdEmailData2 = createEmailEntity("addr2@EXample.com", 15);

		
		when(attrHelper.getAttributeTypeWithSingeltonMetadata(ContactEmailMetadataProvider.NAME)).thenReturn(null);
		
		when(identityDAO.getIdByTypeAndValues(EmailIdentity.ID, List.of("addr1@example.com", "addr2@example.com"))).thenReturn(Set.of(14L, 15L));
		when(entityManagement.getEntity(new EntityParam(14L))).thenReturn(entityWithIdEmailData1);
		when(entityManagement.getGroups(new EntityParam(14L))).thenReturn(Map.of("/", new GroupMembership("/", 15L, new Date()), "/B", new GroupMembership("/B", 14L, new Date())));
		when(entityManagement.getEntity(new EntityParam(15L))).thenReturn(entityWithIdEmailData2);
		when(entityManagement.getGroups(new EntityParam(15L))).thenReturn(Map.of("/", new GroupMembership("/", 15L, new Date()), "/C", new GroupMembership("/C", 14L, new Date())));
	
		
		
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement, entityManagement, identityDAO, attributeDAO);
		Set<EntityWithContactInfo> entityIdByContactAddress = userFinder.getEntitiesIdsByContactAddressesWithDirectAttributeCheck(Set.of("Addr1@examplE.com", "addr2@EXample.com"));
		
		assertThat(entityIdByContactAddress.stream().map(e -> e.entity.getId()).collect(Collectors.toSet())).contains(14L, 15L);	
		assertThat(entityIdByContactAddress.stream().filter(e -> e.entity.getId().equals(14l)).findFirst().get().groups).containsExactlyElementsOf(Set.of("/B", "/"));	
		assertThat(entityIdByContactAddress.stream().filter(e -> e.entity.getId().equals(15l)).findFirst().get().groups).containsExactlyElementsOf(Set.of("/C", "/"));	

	}
	
	
	private Entity createEmailEntity(String email, long entityId) throws IllegalIdentityValueException
	{
		IdentityParam idParam = emailIdType.convertFromString(email, "ridp", null);
		Identity identity = new Identity(idParam, entityId,
				emailIdType.getComparableValue(idParam.getValue(), "realm", null));
		return new Entity(Lists.newArrayList(identity), new EntityInformation(entityId), null);
	}	
}
