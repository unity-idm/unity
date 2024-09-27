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
import java.util.HashMap;
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
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;

public class ExistingUserFinderTest
{
	private static final EmailIdentity emailIdType = new EmailIdentity();
	private BulkGroupQueryService bulkService = mock(BulkGroupQueryService.class);
	private AttributesHelper attrHelper = mock(AttributesHelper.class);
	private GroupsManagement groupsManagement = mock(GroupsManagement.class);
	
	
	@Test
	public void shouldFindByIdentityCaseInsensitive() throws EngineException
	{
		EntityInGroupData entityData = new EntityInGroupData(createEmailEntity("addr1@EXample.com", 13), null, null,
				null, null, null);
		when(bulkService.getMembershipInfo(any())).thenReturn(ImmutableMap.of(13l, entityData));
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement);

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
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement);

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

		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement);
		Set<Entity> entityIdByContactAddress = userFinder.getEntitiesIdsByContactAddress("Addr1@examplE.com");
		assertThat(entityIdByContactAddress.size()).isEqualTo(2);
		assertThat(entityIdByContactAddress.stream().map(e -> e.getId()).collect(Collectors.toSet())).contains(14L, 13L);
	}
	
	
	@Test
	public void shouldFindAllEntitiesWithGivenEmailRespectOnlyDirectAttributes() throws EngineException
	{	
		AttributeExt emailAttr = new AttributeExt(VerifiableEmailAttribute.of(ContactEmailMetadataProvider.NAME, "/", "addr1@EXample.com"), true);
		EntityInGroupData entityWithEmailAttrData = new EntityInGroupData(createEmailEntity("other@example.com", 13),
				null, null, ImmutableMap.of(ContactEmailMetadataProvider.NAME, emailAttr), null, null);
		EntityInGroupData entityWithIdEmailData = new EntityInGroupData(createEmailEntity("addr1@EXample.com", 14),
				null, null, new HashMap<>(), null, null);
		
		when(bulkService.getGroupEntitiesNoContextWithoutTargeted(any())).thenReturn(Map.of(13l, entityWithEmailAttrData.entity, 14l, entityWithIdEmailData.entity));
		
		when(attrHelper.getFirstVerifiableAttributeValueFilteredByMeta(eq(ContactEmailMetadataProvider.NAME),
				eq(Arrays.asList(emailAttr))))
						.thenReturn(Optional.of(VerifiableEmail.fromJsonString(emailAttr.getValues().get(0))));
		when(bulkService.getGroupUsersDirectAttributes(any(), any())).thenReturn(Map.of(13l, Map.of(ContactEmailMetadataProvider.NAME, emailAttr)));	
		GroupContents contents = new GroupContents();
		Group group = new Group("/");
		AttributeStatement statement = new AttributeStatement("true", "/", ConflictResolution.merge, "name", "'name'");
		group.setAttributeStatements(new AttributeStatement[] {statement});
		contents.setGroup(group);	
		when(groupsManagement.getContents("/", GroupContents.METADATA)).thenReturn(contents);
		when(attrHelper.getAttributeTypeWithSingeltonMetadata(ContactEmailMetadataProvider.NAME)).thenReturn(new AttributeType(ContactEmailMetadataProvider.NAME, "email"));
		when(bulkService.getEntitiesGroups(any())).thenReturn(Map.of(13l, Set.of("/","/A"), 14l, Set.of("/", "/B")));
		
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper, groupsManagement);
		Set<EntityWithContactInfo> entityIdByContactAddress = userFinder.getEntitiesIdsByContactAddressesWithDirectAttributeCheck(Set.of("Addr1@examplE.com"));
		assertThat(entityIdByContactAddress.size()).isEqualTo(2);
		assertThat(entityIdByContactAddress.stream().map(e -> e.entity.getId()).collect(Collectors.toSet())).contains(14L, 13L);	
		assertThat(entityIdByContactAddress.stream().filter(e -> e.entity.getId().equals(13l)).findFirst().get().groups).containsExactlyElementsOf(Set.of("/", "/A"));	
		assertThat(entityIdByContactAddress.stream().filter(e -> e.entity.getId().equals(14l)).findFirst().get().groups).containsExactlyElementsOf(Set.of("/", "/B"));	
	}
	
	
	
	private Entity createEmailEntity(String email, long entityId) throws IllegalIdentityValueException
	{
		IdentityParam idParam = emailIdType.convertFromString(email, "ridp", null);
		Identity identity = new Identity(idParam, entityId,
				emailIdType.getComparableValue(idParam.getValue(), "realm", null));
		return new Entity(Lists.newArrayList(identity), new EntityInformation(entityId), null);
	}	
}
