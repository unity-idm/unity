/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.forms.reg;

import static org.assertj.core.api.Assertions.assertThat;


import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementState;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;

public class TestRegistrationPolicyAgreements extends RegistrationTestBase
{
	@Autowired
	private PolicyDocumentManagement policyDocMan;

	@Autowired
	private PolicyAgreementManagement policyAgrMan;

	@Test
	public void policyAgreementShouldBeApplied() throws EngineException
	{
		initContents();
		Long docId = policyDocMan.addPolicyDocument(new PolicyDocumentCreateRequest("test", new I18nString(),
				false, PolicyDocumentContentType.EMBEDDED, new I18nString("test")));
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		RegistrationFormBuilder formBuilder = getFormBuilder(true, "true", false);
		formBuilder.withAddedPolicyAgreement(new PolicyAgreementConfiguration(Arrays.asList(1L),
				PolicyAgreementPresentationType.CHECKBOX_SELECTED,
				new I18nString("{" + docId + ":1}")));

		RegistrationForm form = formBuilder.build();
		registrationsMan.addForm(form);

		RegistrationRequest request = getRequest();
		request.setPolicyAgreements(Arrays.asList(new PolicyAgreementDecision(
				PolicyAgreementAcceptanceStatus.ACCEPTED, Arrays.asList(docId))));
		request.setRegistrationCode(null);
		registrationsMan.submitRegistrationRequest(request, defContext);
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertThat(fromDb.getStatus()).isEqualTo(RegistrationRequestStatus.accepted);

		List<PolicyAgreementState> policyAgreementStatus = policyAgrMan
				.getPolicyAgreementsStatus(new EntityParam(fromDb.getCreatedEntityId()));
		assertThat(policyAgreementStatus.get(0).policyDocumentId).isEqualTo(docId);
		assertThat(policyAgreementStatus.get(0).acceptanceStatus).isEqualTo(PolicyAgreementAcceptanceStatus.ACCEPTED);
	}
}
