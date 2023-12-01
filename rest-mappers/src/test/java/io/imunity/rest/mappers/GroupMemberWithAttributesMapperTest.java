/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.RestGroupMemberWithAttributes;
import io.imunity.rest.api.types.basic.RestAttributeExt;
import io.imunity.rest.api.types.basic.RestEntityInformation;
import io.imunity.rest.api.types.basic.RestIdentity;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberWithAttributes;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Identity;

public class GroupMemberWithAttributesMapperTest
		extends MapperTestBase<GroupMemberWithAttributes, RestGroupMemberWithAttributes>
{

	@Override
	protected GroupMemberWithAttributes getFullAPIObject()
	{
		Identity id = new Identity("email", "email@email.com", 0, "email@email.com");
		id.setCreationTs(new Date(1));
		id.setUpdateTs(new Date(2));
		AttributeExt attributeExt = new AttributeExt(
				new Attribute("attr", "syntax", "/A", List.of("v1", "v2"), "remoteIdp", "translationProfile"), false);
		attributeExt.setCreationTs(new Date(100L));
		attributeExt.setUpdateTs(new Date(1000L));

		return new GroupMemberWithAttributes(new EntityInformation(0), List.of(id), List.of(attributeExt));

	}

	@Override
	protected RestGroupMemberWithAttributes getFullRestObject()
	{
		return RestGroupMemberWithAttributes.builder()
				.withEntityInformation(RestEntityInformation.builder()
						.withState("valid")
						.withEntityId(0L)
						.build())
				.withIdentities(List.of(RestIdentity.builder()
						.withCreationTs(new Date(1))
						.withUpdateTs(new Date(2))
						.withComparableValue("email@email.com")
						.withEntityId(0)
						.withTypeId("email")
						.withValue("email@email.com")
						.withConfirmationInfo(RestConfirmationInfo.builder()
								.withConfirmed(false)
								.build())
						.build()))
				.withAttributes(List.of(RestAttributeExt.builder()
						.withRemoteIdp("remoteIdp")
						.withTranslationProfile("translationProfile")
						.withGroupPath("/A")
						.withValueSyntax("syntax")
						.withValues(List.of("v1", "v2"))
						.withName("attr")
						.withCreationTs(new Date(100L))
						.withUpdateTs(new Date(1000L))
						.withDirect(false)
						.build()))
				.build();
	}

	@Override
	protected Pair<Function<GroupMemberWithAttributes, RestGroupMemberWithAttributes>, Function<RestGroupMemberWithAttributes, GroupMemberWithAttributes>> getMapper()
	{
		return Pair.of(GroupMemberWithAttributesMapper::map, GroupMemberWithAttributesMapper::map);
	}

}
