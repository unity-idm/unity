/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.policyAgreement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementDecision;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementState;
import pl.edu.icm.unity.stdext.attr.PolicyAgreementAttributeSyntax;
import pl.edu.icm.unity.store.api.PolicyDocumentDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;

@Component
public class PolicyAgreementManagementImpl implements PolicyAgreementManagement
{
	private AttributesManagement attrMan;
	private PolicyDocumentDAO policyDocDao;

	public PolicyAgreementManagementImpl(@Qualifier("insecure") AttributesManagement attrMan,
			PolicyDocumentDAO policyDocDao)
	{
		this.attrMan = attrMan;
		this.policyDocDao = policyDocDao;
	}

	@Transactional
	@Override
	public List<PolicyAgreementConfiguration> filterAgreementToPresent(EntityParam entity,
			List<PolicyAgreementConfiguration> toFilter) throws EngineException
	{
		List<PolicyAgreementConfiguration> filteredAgreements = new ArrayList<>();
		List<PolicyAgreementState> states = getPolicyAgreementsStatus(entity);
		Collection<StoredPolicyDocument> policyDocuments = policyDocDao.getAll();
		for (PolicyAgreementConfiguration config : toFilter)
		{
			if (isPolicyAgreementConfigShouldBePresent(config, states, policyDocuments))
			{
				filteredAgreements.add(config);
			}
		}
		return filteredAgreements;
	}

	private boolean isPolicyAgreementConfigShouldBePresent(PolicyAgreementConfiguration config,
			List<PolicyAgreementState> states, Collection<StoredPolicyDocument> policyDocuments)
	{
		for (Long id : config.documentsIdsToAccept)
		{
			Optional<StoredPolicyDocument> doc = policyDocuments.stream().filter(d -> d.getId() == id)
					.findFirst();
			if (!doc.isPresent())
				continue;

			Optional<PolicyAgreementState> status = filterState(id, states);
			if (isDocumentShouldBePreseneted(doc.get(), status))
			{
				return true;
			}
		}

		return false;
	}

	private boolean isDocumentShouldBePreseneted(StoredPolicyDocument doc,
			Optional<PolicyAgreementState> state)
	{
		if (!state.isPresent())
		{
			return true;
		}

		if (doc.isMandatory())
		{
			return isMandatoryDocShouldBePresented(doc, state);
		} else
		{
			return isOptionalDocShouldBePresented(doc, state);
		}

	}

	private boolean isOptionalDocShouldBePresented(StoredPolicyDocument doc,
			Optional<PolicyAgreementState> state)
	{
		PolicyAgreementState stateR = state.get();
		if (stateR.acceptanceStatus == PolicyAgreementAcceptanceStatus.ACCEPTED)
		{
			if (doc.getRevision() > stateR.policyDocumentRevision)
			{
				return true;
			}
		}

		return false;
	}

	boolean isMandatoryDocShouldBePresented(StoredPolicyDocument doc, Optional<PolicyAgreementState> state)
	{
		PolicyAgreementState stateR = state.get();

		if (stateR.acceptanceStatus != PolicyAgreementAcceptanceStatus.ACCEPTED)
		{
			return true;
		}

		if (doc.getRevision() > stateR.policyDocumentRevision)
		{
			return true;
		}

		return false;
	}

	private Optional<PolicyAgreementState> filterState(Long id, List<PolicyAgreementState> states)
	{

		return states.stream().filter(s -> s.policyDocumentId == id).findFirst();
	}

	@Override
	public List<PolicyAgreementState> getPolicyAgreementsStatus(EntityParam entity) throws EngineException
	{
		List<PolicyAgreementState> ret = new ArrayList<>();

		Collection<AttributeExt> attributes = attrMan.getAttributes(entity, "/",
				PolicyAgreementStateAttributeProvider.POLICY_AGREEMENT_STATE);
		if (attributes.isEmpty())
			return ret;

		for (String v : attributes.iterator().next().getValues())
		{
			ret.add(PolicyAgreementState.fromJson(v));
		}

		return ret;
	}

	@Transactional
	@Override
	public void submitDecisions(EntityParam entity, List<PolicyAgreementDecision> decisions) throws EngineException
	{

		Map<Long, StoredPolicyDocument> policyDocuments = policyDocDao.getAll().stream()
				.collect(Collectors.toMap(d -> d.getId(), d -> d));

		List<PolicyAgreementState> states = new ArrayList<>();
		Date time = new Date();
		for (PolicyAgreementDecision decision : (Iterable<PolicyAgreementDecision>) decisions.stream()
				.filter(d -> d != null)::iterator)
		{
			for (Long docId : decision.documentsIdsToAccept)
			{
				states.add(new PolicyAgreementState(docId,
						policyDocuments.get(docId).getRevision(), decision.acceptanceStatus,
						time));
			}

		}
		setState(entity, states);
	}

	private void setState(EntityParam entity, List<PolicyAgreementState> states) throws EngineException
	{

		if (states.isEmpty())
		{
			return;
		}
		Collection<AttributeExt> attributes = attrMan.getAttributes(entity, "/",
				PolicyAgreementStateAttributeProvider.POLICY_AGREEMENT_STATE);
		if (attributes.isEmpty())
		{
			attrMan.setAttribute(entity,
					new Attribute(PolicyAgreementStateAttributeProvider.POLICY_AGREEMENT_STATE,
							PolicyAgreementAttributeSyntax.ID, "/", mapValues(states)));
		} else
		{
			Map<Long, PolicyAgreementState> actual = new HashMap<>();
			for (String orgval : attributes.stream().findFirst().get().getValues())
			{
				PolicyAgreementState value = PolicyAgreementState.fromJson(orgval);
				actual.put(value.policyDocumentId, value);
			}

			for (PolicyAgreementState state : states)
			{
				actual.put(state.policyDocumentId, state);
			}

			List<String> stringVals = new ArrayList<>();
			for (PolicyAgreementState sv : actual.values())
			{
				stringVals.add(sv.toJson());
			}

			attrMan.setAttribute(entity,
					new Attribute(PolicyAgreementStateAttributeProvider.POLICY_AGREEMENT_STATE,
							PolicyAgreementAttributeSyntax.ID, "/",
							mapValues(actual.values())));
		}
	}

	private List<String> mapValues(Collection<PolicyAgreementState> states) throws EngineException
	{
		List<String> stringVals = new ArrayList<>();
		for (PolicyAgreementState sv : states)
		{
			stringVals.add(sv.toJson());
		}

		return stringVals;
	}
}
