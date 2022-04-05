/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import pl.edu.icm.unity.engine.api.groupMember.GroupMemberWithAttributes;
import pl.edu.icm.unity.model.RestAttributeExt;
import pl.edu.icm.unity.model.RestEntityInformation;
import pl.edu.icm.unity.model.RestGroupMemberWithAttributes;
import pl.edu.icm.unity.model.RestIdentity;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Identity;

import java.util.stream.Collectors;

class RestApiMapper
{
	static RestGroupMemberWithAttributes map(GroupMemberWithAttributes groupMemberWithAttributes) {
		return new RestGroupMemberWithAttributes(
				RestApiMapper.map(groupMemberWithAttributes.getEntityInformation()),
				groupMemberWithAttributes.getIdentities().stream()
						.map(RestApiMapper::map)
						.collect(Collectors.toList()),
				groupMemberWithAttributes.getAttributes().stream()
						.map(RestApiMapper::map)
						.collect(Collectors.toList()));
	}

	static RestAttributeExt map(AttributeExt attributeExt) {
		return new RestAttributeExt(
				attributeExt.isDirect(),
				attributeExt.getCreationTs(),
				attributeExt.getUpdateTs(),
				attributeExt.getName(),
				attributeExt.getValueSyntax(),
				attributeExt.getGroupPath(),
				attributeExt.getValues(),
				attributeExt.getTranslationProfile(),
				attributeExt.getRemoteIdp()
		);
	}

	static RestIdentity map(Identity identity) {
		return new RestIdentity(
				identity.getEntityId(),
				identity.getCreationTs(),
				identity.getUpdateTs(),
				identity.getComparableValue()
		);
	}

	static RestEntityInformation map(EntityInformation entityInformation) {
		return new RestEntityInformation(
				entityInformation.getId(),
				entityInformation.getEntityState().name(),
				entityInformation.getScheduledOperationTime(),
				entityInformation.getScheduledOperation().name(),
				entityInformation.getRemovalByUserTime()
		);
	}
}
