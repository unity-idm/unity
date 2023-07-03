/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.req;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.registration.AdminComment;
import pl.edu.icm.unity.base.registration.CredentialParamValue;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.Selection;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.impl.attribute.DBAttribute;
import pl.edu.icm.unity.store.objstore.reg.common.DBAdminComment;
import pl.edu.icm.unity.store.objstore.reg.common.DBCredentialParamValue;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupSelection;
import pl.edu.icm.unity.store.objstore.reg.common.DBIdentityParam;
import pl.edu.icm.unity.store.objstore.reg.common.DBPolicyAgreementDecision;
import pl.edu.icm.unity.store.objstore.reg.common.DBRegistrationContext;
import pl.edu.icm.unity.store.objstore.reg.common.DBSelection;
import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;

public class RegistrationRequestStateMapperTest
		extends MapperTestBase<RegistrationRequestState, DBRegistrationRequestState>
{

	@Override
	protected RegistrationRequestState getFullAPIObject()
	{

		AdminComment adminComment = new AdminComment("comment", 1, true);
		adminComment.setDate(new Date(1));
		RegistrationContext registrationContext = new RegistrationContext(true, TriggeringMode.manualAtLogin);

		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");

		PolicyAgreementDecision policyAgreementDecision = new PolicyAgreementDecision(
				PolicyAgreementAcceptanceStatus.ACCEPTED, List.of(1L, 2L));
		CredentialParamValue credentialParamValue = new CredentialParamValue("credential", "secret");

		GroupSelection groupSelection = new GroupSelection(List.of("/g1", "/g2"));
		groupSelection.setExternalIdp("externalIdp");
		groupSelection.setTranslationProfile("Profile");

		ConfirmationInfo confirmationInfo = new ConfirmationInfo(true);
		confirmationInfo.setSentRequestAmount(1);
		confirmationInfo.setConfirmationDate(1L);

		IdentityParam idParam1 = new IdentityParam("email", "test@wp.pl", "remoteIdp", "Profile");
		confirmationInfo.setSentRequestAmount(1);
		idParam1.setConfirmationInfo(confirmationInfo);
		idParam1.setRealm("realm");
		idParam1.setTarget("target");
		idParam1.setMetadata(meta);

		RegistrationRequest registrationRequest = new RegistrationRequest();
		registrationRequest.setAgreements(List.of(new Selection(true, "externalIdp", "profile")));
		registrationRequest.setAttributes(List.of(new Attribute("attr", "syntax", "/", List.of("v1"))));
		registrationRequest.setComments("comments");
		registrationRequest.setCredentials(List.of(credentialParamValue));
		registrationRequest.setFormId("formId");
		registrationRequest.setGroupSelections(List.of(groupSelection));
		registrationRequest.setIdentities(List.of(idParam1));
		registrationRequest.setPolicyAgreements(List.of(policyAgreementDecision));
		registrationRequest.setRegistrationCode("Code");
		registrationRequest.setUserLocale("en");

		RegistrationRequestState registrationRequestState = new RegistrationRequestState();
		registrationRequestState.setAdminComments(List.of(adminComment));
		registrationRequestState.setCreatedEntityId(1L);
		registrationRequestState.setRegistrationContext(registrationContext);
		registrationRequestState.setRequest(registrationRequest);
		registrationRequestState.setStatus(RegistrationRequestStatus.pending);
		registrationRequestState.setTimestamp(new Date(1));
		registrationRequestState.setRequestId("id");

		return registrationRequestState;

	}

	@Override
	protected DBRegistrationRequestState getFullDBObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");

		return DBRegistrationRequestState.builder()
				.withAdminComments(List.of(DBAdminComment.builder()
						.withAuthorEntityId(1)
						.withContents("comment")
						.withDate(new Date(1))
						.withPublicComment(true)
						.build()))
				.withCreatedEntityId(1L)
				.withRegistrationContext(DBRegistrationContext.builder()
						.withIsOnIdpEndpoint(true)
						.withTriggeringMode("manualAtLogin")
						.build())
				.withRequestId("id")
				.withStatus("pending")
				.withTimestamp(new Date(1))
				.withRequest(DBRegistrationRequest.builder()
						.withAgreements(List.of(DBSelection.builder()
								.withExternalIdp("externalIdp")
								.withSelected(true)
								.withTranslationProfile("profile")
								.build()))
						.withAttributes(List.of(DBAttribute.builder()
								.withName("attr")
								.withValueSyntax("syntax")
								.withGroupPath("/")
								.withValues(List.of("v1"))
								.build()))
						.withComments("comments")
						.withCredentials(List.of(DBCredentialParamValue.builder()
								.withCredentialId("credential")
								.withSecrets("secret")
								.build()))
						.withFormId("formId")
						.withGroupSelections(List.of(DBGroupSelection.builder()
								.withExternalIdp("externalIdp")
								.withTranslationProfile("Profile")
								.withSelectedGroups(List.of("/g1", "/g2"))
								.build()))
						.withIdentities(List.of(DBIdentityParam.builder()
								.withValue("test@wp.pl")
								.withTypeId("email")
								.withRealm("realm")
								.withRemoteIdp("remoteIdp")
								.withTarget("target")
								.withMetadata(meta)
								.withTranslationProfile("Profile")
								.withConfirmationInfo(DBConfirmationInfo.builder()
										.withSentRequestAmount(1)
										.withConfirmed(true)
										.withConfirmationDate(1L)
										.build())
								.build()))
						.withPolicyAgreements(List.of(DBPolicyAgreementDecision.builder()
								.withAcceptanceStatus("ACCEPTED")
								.withDocumentsIdsToAccept(List.of(1L, 2L))
								.build()))
						.withRegistrationCode("Code")
						.withUserLocale("en")
						.build())

				.build();
	}

	@Override
	protected Pair<Function<RegistrationRequestState, DBRegistrationRequestState>, Function<DBRegistrationRequestState, RegistrationRequestState>> getMapper()
	{
		return Pair.of(RegistrationRequestStateMapper::map, RegistrationRequestStateMapper::map);
	}

}
