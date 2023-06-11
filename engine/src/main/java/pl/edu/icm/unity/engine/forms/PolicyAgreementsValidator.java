/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.forms;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException.Category;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.BaseRegistrationInput;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.store.api.PolicyDocumentDAO;

@Component
public class PolicyAgreementsValidator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FORMS, PolicyAgreementsValidator.class);

	private PolicyDocumentDAO policyDocDao;
	private PolicyAgreementManagement policyAgrMan;

	@Autowired
	PolicyAgreementsValidator(PolicyDocumentDAO policyDocDao, PolicyAgreementManagement policyAgrMan)
	{
		this.policyDocDao = policyDocDao;
		this.policyAgrMan = policyAgrMan;
	}

	public void validate(EntityParam entity, BaseForm form, BaseRegistrationInput input)
			throws IllegalFormContentsException
	{
		
		if (form.getPolicyAgreements().isEmpty())
		{
			return;
		}
		
		List<PolicyAgreementConfiguration> filterAgreementToPresent;
		try
		{
			filterAgreementToPresent = policyAgrMan.filterAgreementToPresent(entity,
					form.getPolicyAgreements());
		} catch (EngineException e)
		{
			log.error("Can not filer agreements to validate request, all configured are taken");
			filterAgreementToPresent = form.getPolicyAgreements();
		}	
		validate(filterAgreementToPresent, form.getPolicyAgreements(), input.getPolicyAgreements());
	}

	public void validate(BaseForm form, BaseRegistrationInput input) throws IllegalFormContentsException
	{
		if (form.getPolicyAgreements().isEmpty())
		{
			return;
		}
		
		validate(form.getPolicyAgreements(), form.getPolicyAgreements(), input.getPolicyAgreements());
	}

	private void validate(List<PolicyAgreementConfiguration> filterAgreementToPresent,
			List<PolicyAgreementConfiguration> all, List<PolicyAgreementDecision> decisions)
			throws IllegalFormContentsException
	{

		for (PolicyAgreementConfiguration config : filterAgreementToPresent)
		{
			Set<Long> mandatory = filterAllMandatoryDocIds(config);
			decisions.stream().filter(
					d -> d != null && d.acceptanceStatus.equals(PolicyAgreementAcceptanceStatus.ACCEPTED))
					.forEach(d -> mandatory.removeAll(d.documentsIdsToAccept));
			if (!mandatory.isEmpty())
			{
				throw new IllegalFormContentsException("Mandatory policy agreement is not accepted.",
						all.indexOf(config), Category.POLICY_AGREEMENT);
			}
		}
	}

	private Set<Long> filterAllMandatoryDocIds(PolicyAgreementConfiguration config)
	{
		return policyDocDao.getAll().stream()
				.filter(d -> config.documentsIdsToAccept.contains(d.getId()) && d.isMandatory())
				.map(d -> d.getId()).collect(Collectors.toSet());
	}
}
