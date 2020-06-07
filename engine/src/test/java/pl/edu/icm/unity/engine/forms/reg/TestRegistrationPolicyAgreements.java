/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.forms.reg;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementState;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;

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
		assertThat(fromDb.getStatus(), is(RegistrationRequestStatus.accepted));

		List<PolicyAgreementState> policyAgreementStatus = policyAgrMan
				.getPolicyAgreementsStatus(new EntityParam(fromDb.getCreatedEntityId()));
		assertThat(policyAgreementStatus.get(0).policyDocumentId, is(docId));
		assertThat(policyAgreementStatus.get(0).acceptanceStatus, is(PolicyAgreementAcceptanceStatus.ACCEPTED));
	}
}
