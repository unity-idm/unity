/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.store.impl.attribute.AttributeMapper;
import pl.edu.icm.unity.store.impl.attribute.DBAttribute;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupSelection;
import pl.edu.icm.unity.store.objstore.reg.common.DBIdentityParam;
import pl.edu.icm.unity.store.objstore.reg.common.GroupSelectionMapper;
import pl.edu.icm.unity.store.objstore.reg.common.IdentityParamMapper;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;

public class FormPrefillMapper
{
	public static DBFormPrefill map(FormPrefill formPrefill)
	{
		return DBFormPrefill.builder()
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
												.map(e -> PrefilledEntryMapper.<Attribute, DBAttribute>map(e,
														AttributeMapper::map))
												.orElse(null))

								))
						.orElse(null))
				.withIdentities(Optional.ofNullable(formPrefill.getIdentities())
						.map(attributes -> attributes.entrySet()
								.stream()
								.collect(Collectors.toMap(identityEntry -> identityEntry.getKey(),
										identityEntry -> Optional.ofNullable(identityEntry.getValue())
												.map(e -> PrefilledEntryMapper.<IdentityParam, DBIdentityParam>map(e,
														IdentityParamMapper::map))
												.orElse(null))

								))
						.orElse(null))
				.withGroupSelections(Optional.ofNullable(formPrefill.getGroupSelections())
						.map(attributes -> attributes.entrySet()
								.stream()
								.collect(Collectors.toMap(groupEntry -> groupEntry.getKey(),
										groupEntry -> Optional.ofNullable(groupEntry.getValue())
												.map(e -> PrefilledEntryMapper.<GroupSelection, DBGroupSelection>map(
														e, GroupSelectionMapper::map))
												.orElse(null))

								))
						.orElse(null))
				.build();
	}

	public static FormPrefill map(DBFormPrefill restFormPrefill)
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
								.map(e -> PrefilledEntryMapper.<DBAttribute, Attribute>map(e, AttributeMapper::map))
								.orElse(null))))
				.orElse(new HashMap<>()));
		formPrefill.setIdentities(Optional.ofNullable(restFormPrefill.identities)
				.map(attributes -> attributes.entrySet()
						.stream()
						.collect(Collectors.toMap(identityEntry -> identityEntry.getKey(),
								identityEntry -> Optional.ofNullable(identityEntry.getValue())
										.map(e -> PrefilledEntryMapper.<DBIdentityParam, IdentityParam>map(e,
												IdentityParamMapper::map))
										.orElse(null))

						))
				.orElse(new HashMap<>()));
		formPrefill.setGroupSelections(Optional.ofNullable(restFormPrefill.groupSelections)
				.map(attributes -> attributes.entrySet()
						.stream()
						.collect(Collectors.toMap(groupEntry -> groupEntry.getKey(),
								groupEntry -> Optional.ofNullable(groupEntry.getValue())
										.map(e -> PrefilledEntryMapper.<DBGroupSelection, GroupSelection>map(e,
												GroupSelectionMapper::map))
										.orElse(null))

						))
				.orElse(new HashMap<>()));

		return formPrefill;
	}

}
