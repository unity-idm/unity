/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration.invite;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import io.imunity.rest.api.types.registration.RestGroupSelection;
import io.imunity.rest.api.types.registration.invite.RestFormPrefill;
import io.imunity.rest.api.types.registration.invite.RestPrefilledEntry;
import io.imunity.rest.mappers.MapperWithMinimalTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;

public class FormPrefillMapperTest extends MapperWithMinimalTestBase<FormPrefill, RestFormPrefill>
{

	@Override
	protected FormPrefill getFullAPIObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");
		GroupSelection groupSelection = new GroupSelection(List.of("/g1", "/g2"));
		groupSelection.setExternalIdp("externalIdp");
		groupSelection.setTranslationProfile("Profile");
		Attribute attr = new Attribute("attr", "syntax", "/A", Lists.newArrayList("v1", "v2"), "remoteIdp",
				"translationProfile");
		ConfirmationInfo confirmationInfo = new ConfirmationInfo(true);
		confirmationInfo.setSentRequestAmount(1);
		confirmationInfo.setConfirmationDate(1L);

		IdentityParam idParam1 = new IdentityParam("email", "test@wp.pl", "remoteIdp", "Profile");
		confirmationInfo.setSentRequestAmount(1);
		idParam1.setConfirmationInfo(confirmationInfo);
		idParam1.setRealm("realm");
		idParam1.setTarget("target");
		idParam1.setMetadata(meta);

		FormPrefill formPrefill = new FormPrefill();
		formPrefill.setAllowedGroups(Map.of(1, groupSelection));
		formPrefill.setAttributes(Map.of(1, new PrefilledEntry<Attribute>(attr, PrefilledEntryMode.READ_ONLY)));
		formPrefill.setFormId("formId");
		formPrefill.setGroupSelections(
				Map.of(1, new PrefilledEntry<GroupSelection>(groupSelection, PrefilledEntryMode.READ_ONLY)));
		formPrefill.setIdentities(Map.of(1, new PrefilledEntry<IdentityParam>(idParam1, PrefilledEntryMode.HIDDEN)));
		formPrefill.setMessageParams(Map.of("mpk1", "mpv1"));
		return formPrefill;

	}

	@Override
	protected RestFormPrefill getFullRestObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");

		return RestFormPrefill.builder()
				.withFormId("formId")
				.withAllowedGroups(Map.of(1, RestGroupSelection.builder()
						.withExternalIdp("externalIdp")
						.withTranslationProfile("Profile")
						.withSelectedGroups(List.of("/g1", "/g2"))
						.build()))
				.withAttributes(Map.of(1,
						new RestPrefilledEntry.Builder<RestAttribute>().withEntry(RestAttribute.builder()
								.withName("attr")
								.withValueSyntax("syntax")
								.withGroupPath("/A")
								.withValues(List.of("v1", "v2"))
								.withRemoteIdp("remoteIdp")
								.withTranslationProfile("translationProfile")
								.build())
								.withMode("READ_ONLY")
								.build()))
				.withGroupSelections(Map.of(1,
						new RestPrefilledEntry.Builder<RestGroupSelection>().withEntry(RestGroupSelection.builder()
								.withExternalIdp("externalIdp")
								.withTranslationProfile("Profile")
								.withSelectedGroups(List.of("/g1", "/g2"))
								.build())
								.withMode("READ_ONLY")
								.build()))
				.withMessageParams(Map.of("mpk1", "mpv1"))
				.withIdentities(
						Map.of(1,
								new RestPrefilledEntry.Builder<RestIdentityParam>()
										.withEntry(RestIdentityParam.builder()
												.withValue("test@wp.pl")
												.withTypeId("email")
												.withRealm("realm")
												.withRemoteIdp("remoteIdp")
												.withTarget("target")
												.withMetadata(meta)
												.withTranslationProfile("Profile")
												.withConfirmationInfo(RestConfirmationInfo.builder()
														.withSentRequestAmount(1)
														.withConfirmed(true)
														.withConfirmationDate(1L)
														.build())
												.build())
										.withMode("HIDDEN")
										.build()))
				.build();

	}
	
	@Override
	protected FormPrefill getMinAPIObject()
	{
		
		return  new FormPrefill("form", null);
	}

	@Override
	protected RestFormPrefill getMinRestObject()
	{
		return RestFormPrefill.builder().withFormId("form").build();
	}

	@Override
	protected Pair<Function<FormPrefill, RestFormPrefill>, Function<RestFormPrefill, FormPrefill>> getMapper()
	{
		return Pair.of(FormPrefillMapper::map, FormPrefillMapper::map);
	}

	

}
