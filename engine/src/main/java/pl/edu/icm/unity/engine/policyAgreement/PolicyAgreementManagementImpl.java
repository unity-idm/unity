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

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

@Component
public class PolicyAgreementManagementImpl implements PolicyAgreementManagement
{
	public static final String POLICY_AGREEMENT_STATE = "sys:policy-agreement-state";

	private AttributesManagement attrMan;
	private PolicyDocumentManagement policyDocMan;

	public PolicyAgreementManagementImpl(@Qualifier("insecure") AttributesManagement attrMan,
			PolicyDocumentManagement policyDocMan)
	{
		this.attrMan = attrMan;
		this.policyDocMan = policyDocMan;
	}

	@Override
	public List<PolicyAgreementConfiguration> filterAgreementToPresent(EntityParam entity,
			List<PolicyAgreementConfiguration> toFilter) throws EngineException
	{

		List<PolicyAgreementConfiguration> filteredAgreements = new ArrayList<>();
		List<PolicyAgreementStateValue> states = getState(entity);
		Collection<PolicyDocumentWithRevision> policyDocuments = policyDocMan.getPolicyDocuments();
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
			List<PolicyAgreementStateValue> states, Collection<PolicyDocumentWithRevision> policyDocuments)
	{
		for (Long id : config.documentsIdsToAccept)
		{
			Optional<PolicyDocumentWithRevision> doc = policyDocuments.stream().filter(d -> d.id == id)
					.findFirst();
			if (!doc.isPresent())
				continue;

			Optional<PolicyAgreementStateValue> status = filterState(id, states);
			if (isDocumentShouldBePreseneted(doc.get(), status))
			{
				return true;
			}
		}

		return false;
	}

	private boolean isDocumentShouldBePreseneted(PolicyDocumentWithRevision doc,
			Optional<PolicyAgreementStateValue> state)
	{
		if (!state.isPresent())
		{
			return true;
		}

		if (doc.mandatory)
		{
			return isMandatoryDocShouldBePresented(doc, state);
		} else
		{
			return isOptionalDocShouldBePresented(doc, state);
		}

	}

	private boolean isOptionalDocShouldBePresented(PolicyDocumentWithRevision doc,
			Optional<PolicyAgreementStateValue> state)
	{
		PolicyAgreementStateValue stateR = state.get();
		if (stateR.acceptanceStatus == PolicyAgreementAcceptanceStatus.ACCEPTED)
		{
			if (doc.revision > stateR.policyDocumentRevision)
			{
				return true;
			}
		}

		return false;
	}

	boolean isMandatoryDocShouldBePresented(PolicyDocumentWithRevision doc,
			Optional<PolicyAgreementStateValue> state)
	{
		PolicyAgreementStateValue stateR = state.get();

		if (stateR.acceptanceStatus != PolicyAgreementAcceptanceStatus.ACCEPTED)
		{
			return true;
		}

		if (doc.revision > stateR.policyDocumentRevision)
		{
			return true;
		}

		return false;
	}

	private Optional<PolicyAgreementStateValue> filterState(Long id, List<PolicyAgreementStateValue> states)
	{

		return states.stream().filter(s -> s.policyDocumentId == id).findFirst();
	}

	private List<PolicyAgreementStateValue> getState(EntityParam entity) throws EngineException
	{
		List<PolicyAgreementStateValue> ret = new ArrayList<>();

		Collection<AttributeExt> attributes = attrMan.getAttributes(entity, "/", POLICY_AGREEMENT_STATE);
		if (attributes.isEmpty())
			return ret;

		for (String v : attributes.iterator().next().getValues())
		{
			ret.add(PolicyAgreementStateValue.fromJson(v));
		}

		return ret;
	}

	@Override
	public void submitDecisions(EntityParam entity, List<PolicyAgreementDecision> decisions) throws EngineException
	{

		Map<Long, PolicyDocumentWithRevision> policyDocuments = policyDocMan.getPolicyDocuments().stream()
				.collect(Collectors.toMap(d -> d.id, d -> d));

		List<PolicyAgreementStateValue> states = new ArrayList<>();

		Date time = new Date();

		for (PolicyAgreementDecision decision : decisions)
		{
			for (Long docId : decision.documentsIdsToAccept)
			{
				states.add(new PolicyAgreementStateValue(docId, policyDocuments.get(docId).revision,
						decision.acceptanceStatus, time));
			}

		}

		setState(entity, states);

	}

	private void setState(EntityParam entity, List<PolicyAgreementStateValue> states) throws EngineException
	{

		Collection<AttributeExt> attributes = attrMan.getAttributes(entity, "/", POLICY_AGREEMENT_STATE);
		if (attributes.isEmpty())
		{
			attrMan.setAttribute(entity,
					StringAttribute.of(POLICY_AGREEMENT_STATE, "/", mapValues(states)));
		} else
		{
			Map<Long, PolicyAgreementStateValue> actual = new HashMap<>();
			for (String orgval : attributes.stream().findFirst().get().getValues())
			{
				PolicyAgreementStateValue value = PolicyAgreementStateValue.fromJson(orgval);
				actual.put(value.policyDocumentId, value);
			}

			for (PolicyAgreementStateValue state : states)
			{
				actual.put(state.policyDocumentId, state);
			}

			List<String> stringVals = new ArrayList<>();
			for (PolicyAgreementStateValue sv : actual.values())
			{
				stringVals.add(sv.toJson());
			}

			attrMan.setAttribute(entity,
					StringAttribute.of(POLICY_AGREEMENT_STATE, "/", mapValues(actual.values())));
		}
	}

	private List<String> mapValues(Collection<PolicyAgreementStateValue> states) throws EngineException
	{
		List<String> stringVals = new ArrayList<>();
		for (PolicyAgreementStateValue sv : states)
		{
			stringVals.add(sv.toJson());
		}

		return stringVals;
	}
}
