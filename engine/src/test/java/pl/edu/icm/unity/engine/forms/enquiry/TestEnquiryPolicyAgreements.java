/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.forms.enquiry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementState;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class TestEnquiryPolicyAgreements extends DBIntegrationTestBase
{
	@Autowired
	private EnquiryManagement enquiryMan;

	@Autowired
	private PolicyDocumentManagement policyDocMan;

	@Autowired
	private PolicyAgreementManagement policyAgrMan;

	@Test
	public void policyAgreementShouldBeApplied() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		Long docId = policyDocMan.addPolicyDocument(new PolicyDocumentCreateRequest("test", new I18nString(),
				false, PolicyDocumentContentType.EMBEDDED, new I18nString("test")));

		EnquiryForm form = new EnquiryFormBuilder().withName("f1").withDescription("desc")
				.withTargetGroups(new String[] { "/" }).withType(EnquiryType.REQUESTED_MANDATORY)
				.withCollectComments(true).withFormInformation(new I18nString("formInformation"))
				.withAddedCredentialParam(new CredentialRegistrationParam(
						EngineInitialization.DEFAULT_CREDENTIAL))
				.withAddedIdentityParam().withIdentityType(UsernameIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive).endIdentityParam()
				.withAddedPolicyAgreement(new PolicyAgreementConfiguration(Arrays.asList(1L),
						PolicyAgreementPresentationType.CHECKBOX_SELECTED,
						new I18nString("{" + docId + ":1}")))
				.build();
		enquiryMan.addEnquiry(form);
		EnquiryResponse response = new EnquiryResponseBuilder().withFormId("f1").withComments("comments")
				.withAddedCredential().withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abc").toJson()).endCredential()
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "test-user"))
				.withAddedPolicyAgreement(new PolicyAgreementDecision(
						PolicyAgreementAcceptanceStatus.ACCEPTED, Arrays.asList(docId)))
				.build();
		String id = enquiryMan.submitEnquiryResponse(response, defContext);
		enquiryMan.processEnquiryResponse(id, response, RegistrationRequestAction.accept, "", "");

		EnquiryResponseState fromDb = enquiryMan.getEnquiryResponse(id);

		assertThat(fromDb.getStatus(), is(RegistrationRequestStatus.accepted));

		List<PolicyAgreementState> policyAgreementStatus = policyAgrMan
				.getPolicyAgreementsStatus(new EntityParam(fromDb.getEntityId()));
		assertThat(policyAgreementStatus.get(0).policyDocumentId, is(docId));
		assertThat(policyAgreementStatus.get(0).acceptanceStatus, is(PolicyAgreementAcceptanceStatus.ACCEPTED));
	}
}
