/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementDecision;
import io.imunity.rest.api.types.registration.RestCredentialParamValue;
import io.imunity.rest.api.types.registration.RestGroupSelection;
import io.imunity.rest.api.types.registration.RestRegistrationRequest;
import io.imunity.rest.api.types.registration.RestSelection;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.Selection;

public class RegistrationRequestMapperTest extends MapperTestBase<RegistrationRequest, RestRegistrationRequest>
{

	@Override
	protected RegistrationRequest getFullAPIObject()
	{
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
		return registrationRequest;
	}

	@Override
	protected RestRegistrationRequest getFullRestObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");

		return RestRegistrationRequest.builder()
				.withAgreements(List.of(RestSelection.builder()
						.withExternalIdp("externalIdp")
						.withSelected(true)
						.withTranslationProfile("profile")
						.build()))
				.withAttributes(List.of(RestAttribute.builder()
						.withName("attr")
						.withValueSyntax("syntax")
						.withGroupPath("/")
						.withValues(List.of("v1"))
						.build()))
				.withComments("comments")
				.withCredentials(List.of(RestCredentialParamValue.builder()
						.withCredentialId("credential")
						.withSecrets("secret")
						.build()))
				.withFormId("formId")
				.withGroupSelections(List.of(RestGroupSelection.builder()
						.withExternalIdp("externalIdp")
						.withTranslationProfile("Profile")
						.withSelectedGroups(List.of("/g1", "/g2"))
						.build()))
				.withIdentities(List.of(RestIdentityParam.builder()
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
						.build()))
				.withPolicyAgreements(List.of(RestPolicyAgreementDecision.builder()
						.withAcceptanceStatus("ACCEPTED")
						.withDocumentsIdsToAccept(List.of(1L, 2L))
						.build()))
				.withRegistrationCode("Code")
				.withUserLocale("en")
				.build();
	}

	@Override
	protected Pair<Function<RegistrationRequest, RestRegistrationRequest>, Function<RestRegistrationRequest, RegistrationRequest>> getMapper()
	{
		return Pair.of(RegistrationRequestMapper::map, RegistrationRequestMapper::map);
	}

}
