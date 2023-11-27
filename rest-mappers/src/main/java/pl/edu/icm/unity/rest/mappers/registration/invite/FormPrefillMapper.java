/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration.invite;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.registration.RestGroupSelection;
import io.imunity.rest.api.types.registration.invite.RestFormPrefill;
import pl.edu.icm.unity.rest.mappers.AttributeMapper;
import pl.edu.icm.unity.rest.mappers.IdentityParamMapper;
import pl.edu.icm.unity.rest.mappers.registration.GroupSelectionMapper;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;

public class FormPrefillMapper
{
	public static RestFormPrefill map(FormPrefill formPrefill)
	{
		return RestFormPrefill.builder()
				.withAllowedGroups(Optional.ofNullable(formPrefill.getAllowedGroups())
						.map(allowedGroupsMap -> allowedGroupsMap.entrySet()
								.stream()
								.collect(Collectors.toMap(e -> e.getKey(), e -> Optional.ofNullable(e.getValue())
										.map(GroupSelectionMapper::map)
										.orElse(null))))
						.orElse(null))
				.withFormId(formPrefill.getFormId())
				.withMessageParams(formPrefill.getMessageParams())
				.withAttributes(Optional.ofNullable(formPrefill.getAttributes())
						.map(attributes -> attributes.entrySet()
								.stream()
								.collect(Collectors.toMap(attributeEntry -> attributeEntry.getKey(),
										attributeEntry -> Optional.ofNullable(attributeEntry.getValue())
												.map(e -> PrefilledEntryMapper.<Attribute, RestAttribute>map(e,
														AttributeMapper::map))
												.orElse(null))

								))
						.orElse(null))
				.withIdentities(Optional.ofNullable(formPrefill.getIdentities())
						.map(attributes -> attributes.entrySet()
								.stream()
								.collect(Collectors.toMap(identityEntry -> identityEntry.getKey(),
										identityEntry -> Optional.ofNullable(identityEntry.getValue())
												.map(e -> PrefilledEntryMapper.<IdentityParam, RestIdentityParam>map(e,
														IdentityParamMapper::map))
												.orElse(null))

								))
						.orElse(null))
				.withGroupSelections(Optional.ofNullable(formPrefill.getGroupSelections())
						.map(attributes -> attributes.entrySet()
								.stream()
								.collect(Collectors.toMap(groupEntry -> groupEntry.getKey(),
										groupEntry -> Optional.ofNullable(groupEntry.getValue())
												.map(e -> PrefilledEntryMapper.<GroupSelection, RestGroupSelection>map(
														e, GroupSelectionMapper::map))
												.orElse(null))

								))
						.orElse(null))
				.build();
	}

	public static FormPrefill map(RestFormPrefill restFormPrefill)
	{
		FormPrefill formPrefill = new FormPrefill();
		formPrefill.setFormId(restFormPrefill.formId);
		formPrefill.setMessageParams(Optional.ofNullable(restFormPrefill.messageParams).orElse(new HashMap<>()));
		formPrefill.setAllowedGroups(Optional.ofNullable(restFormPrefill.allowedGroups)
				.map(allowedGroupsMap -> allowedGroupsMap.entrySet()
						.stream()
						.collect(Collectors.toMap(e -> e.getKey(), e -> Optional.ofNullable(e.getValue())
								.map(GroupSelectionMapper::map)
								.orElse(null))))
				.orElse(new HashMap<>()));
		formPrefill.setAttributes(Optional.ofNullable(restFormPrefill.attributes)
				.map(attributes -> attributes.entrySet()
						.stream()
						.collect(Collectors.toMap(attributeEntry -> attributeEntry.getKey(), attributeEntry -> Optional
								.ofNullable(attributeEntry.getValue())
								.map(e -> PrefilledEntryMapper.<RestAttribute, Attribute>map(e, AttributeMapper::map))
								.orElse(null))))
				.orElse(new HashMap<>()));
		formPrefill.setIdentities(Optional.ofNullable(restFormPrefill.identities)
				.map(attributes -> attributes.entrySet()
						.stream()
						.collect(Collectors.toMap(identityEntry -> identityEntry.getKey(),
								identityEntry -> Optional.ofNullable(identityEntry.getValue())
										.map(e -> PrefilledEntryMapper.<RestIdentityParam, IdentityParam>map(e,
												IdentityParamMapper::map))
										.orElse(null))

						))
				.orElse(new HashMap<>()));
		formPrefill.setGroupSelections(Optional.ofNullable(restFormPrefill.groupSelections)
				.map(attributes -> attributes.entrySet()
						.stream()
						.collect(Collectors.toMap(groupEntry -> groupEntry.getKey(),
								groupEntry -> Optional.ofNullable(groupEntry.getValue())
										.map(e -> PrefilledEntryMapper.<RestGroupSelection, GroupSelection>map(e,
												GroupSelectionMapper::map))
										.orElse(null))

						))
				.orElse(new HashMap<>()));

		return formPrefill;
	}

}
