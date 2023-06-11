/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.req;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.registration.CredentialParamValue;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.Selection;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.impl.attribute.DBAttribute;
import pl.edu.icm.unity.store.objstore.reg.common.DBCredentialParamValue;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupSelection;
import pl.edu.icm.unity.store.objstore.reg.common.DBIdentityParam;
import pl.edu.icm.unity.store.objstore.reg.common.DBPolicyAgreementDecision;
import pl.edu.icm.unity.store.objstore.reg.common.DBSelection;
import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;

public class RegistrationRequestMapperTest extends MapperTestBase<RegistrationRequest, DBRegistrationRequest>
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
	protected DBRegistrationRequest getFullDBObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");

		return DBRegistrationRequest.builder()
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
				.build();
	}

	@Override
	protected Pair<Function<RegistrationRequest, DBRegistrationRequest>, Function<DBRegistrationRequest, RegistrationRequest>> getMapper()
	{
		return Pair.of(RegistrationRequestMapper::map, RegistrationRequestMapper::map);
	}

}
