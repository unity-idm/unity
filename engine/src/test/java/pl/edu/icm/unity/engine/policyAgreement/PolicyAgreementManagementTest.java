/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.policyAgreement;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementState;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class PolicyAgreementManagementTest extends DBIntegrationTestBase
{
	@Autowired
	private PolicyAgreementManagement agreementMan;

	@Autowired
	private PolicyDocumentManagement policyDocMan;

	@Test
	public void shouldSaveDecisionAndReturnStatus() throws EngineException
	{
		Long doc1 = addDoc("1", true);
		EntityParam user = addUser();
		agreementMan.submitDecisions(user,
				Arrays.asList(new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.ACCEPTED,
						Arrays.asList(doc1))));
		List<PolicyAgreementState> policyAgreementsStatus = agreementMan.getPolicyAgreementsStatus(user);
		
		assertThat(policyAgreementsStatus.size(), is(1));
		assertThat(policyAgreementsStatus.get(0).acceptanceStatus, is(PolicyAgreementAcceptanceStatus.ACCEPTED));
		assertThat(policyAgreementsStatus.get(0).policyDocumentId, is(doc1));
	}
		
	@Test
	public void shouldNotFilterAgreements() throws EngineException
	{
		Long doc1 = addDoc("1", true);
		Long doc2 = addDoc("2", true);
		Long doc3 = addDoc("3", false);

		EntityParam user = addUser();
		List<PolicyAgreementConfiguration> filterAgreementToPresent = agreementMan.filterAgreementToPresent(
				user,
				Arrays.asList(getConfig(Arrays.asList(doc1, doc2)), getConfig(Arrays.asList(doc3))));

		assertThat(filterAgreementToPresent.size(), is(2));
		assertThat(filterAgreementToPresent.get(0).documentsIdsToAccept, is(Arrays.asList(doc1, doc2)));
		assertThat(filterAgreementToPresent.get(1).documentsIdsToAccept, is(Arrays.asList(doc3)));
	}

	@Test
	public void shouldFilterOnlyNotConfirmedAgreements() throws EngineException
	{
		Long doc1 = addDoc("1", true);
		Long doc2 = addDoc("2", true);

		EntityParam user = addUser();
		agreementMan.submitDecisions(user,
				Arrays.asList(new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.ACCEPTED,
						Arrays.asList(doc1))));

		List<PolicyAgreementConfiguration> filterAgreementToPresent = agreementMan.filterAgreementToPresent(
				user, Arrays.asList(getConfig(Arrays.asList(doc1)), getConfig(Arrays.asList(doc2))));

		assertThat(filterAgreementToPresent.size(), is(1));
		assertThat(filterAgreementToPresent.get(0).documentsIdsToAccept, is(Arrays.asList(doc2)));	
	}

	@Test
	public void shouldFilterOnlyAgreementsWithNotConfirmedDoc() throws EngineException
	{
		Long doc1 = addDoc("1", true);
		Long doc2 = addDoc("2", true);
		Long doc3 = addDoc("3", true);

		EntityParam user = addUser();
		agreementMan.submitDecisions(user,
				Arrays.asList(new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.ACCEPTED,
						Arrays.asList(doc1))));

		List<PolicyAgreementConfiguration> filterAgreementToPresent = agreementMan.filterAgreementToPresent(
				user,
				Arrays.asList(getConfig(Arrays.asList(doc1, doc2)), getConfig(Arrays.asList(doc3))));

		assertThat(filterAgreementToPresent.size(), is(2));
		assertThat(filterAgreementToPresent.get(0).documentsIdsToAccept, is(Arrays.asList(doc1, doc2)));
		assertThat(filterAgreementToPresent.get(1).documentsIdsToAccept, is(Arrays.asList(doc3)));
	}

	@Test
	public void shouldFilterOnlyAgreementsWithDocWithHigherRevision() throws EngineException
	{
		Long doc1 = addDoc("1", true);
		Long doc2 = addDoc("2", true);
		Long doc3 = addDoc("3", true);

		EntityParam user = addUser();
		agreementMan.submitDecisions(user,
				Arrays.asList(new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.ACCEPTED,
						Arrays.asList(doc1, doc2, doc3))));

		policyDocMan.updatePolicyDocumentWithRevision(
				new PolicyDocumentUpdateRequest(doc1, "up", new I18nString("x"), true,
						PolicyDocumentContentType.EMBEDDED, new I18nString("content")));

		List<PolicyAgreementConfiguration> filterAgreementToPresent = agreementMan.filterAgreementToPresent(
				user,
				Arrays.asList(getConfig(Arrays.asList(doc1, doc2)), getConfig(Arrays.asList(doc3))));

		assertThat(filterAgreementToPresent.size(), is(1));
		assertThat(filterAgreementToPresent.get(0).documentsIdsToAccept, is(Arrays.asList(doc1, doc2)));
	}

	@Test
	public void shouldSkipAgreementsWithOptionalRejetedDocWithHigherRevision() throws EngineException
	{
		Long doc1 = addDoc("1", false);
		Long doc2 = addDoc("2", true);
		Long doc3 = addDoc("3", false);

		EntityParam user = addUser();
		agreementMan.submitDecisions(user,
				Arrays.asList(new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.ACCEPTED,
						Arrays.asList(doc1))));
		agreementMan.submitDecisions(user,
				Arrays.asList(new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.NOT_ACCEPTED,
						Arrays.asList(doc3))));

		policyDocMan.updatePolicyDocumentWithRevision(
				new PolicyDocumentUpdateRequest(doc3, "up", new I18nString("x"), false,
						PolicyDocumentContentType.EMBEDDED, new I18nString("content")));

		List<PolicyAgreementConfiguration> filterAgreementToPresent = agreementMan
				.filterAgreementToPresent(user, Arrays.asList(getConfig(Arrays.asList(doc1)),
						getConfig(Arrays.asList(doc2)), getConfig(Arrays.asList(doc3))));

		assertThat(filterAgreementToPresent.size(), is(1));
		assertThat(filterAgreementToPresent.get(0).documentsIdsToAccept, is(Arrays.asList(doc2)));
	}
	
	private Long addDoc(String name, boolean mandatory) throws EngineException
	{
		return policyDocMan.addPolicyDocument(new PolicyDocumentCreateRequest(name, new I18nString(name),
				mandatory, PolicyDocumentContentType.EMBEDDED, new I18nString("content")));

	}

	private PolicyAgreementConfiguration getConfig(List<Long> docs)
	{
		return new PolicyAgreementConfiguration(docs, PolicyAgreementPresentationType.CHECKBOX_SELECTED,
				new I18nString("Empty"));
	}

	private EntityParam addUser() throws EngineException
	{
		setupPasswordAuthn();
		return new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), CRED_REQ_PASS,
				EntityState.valid));
	}
}
