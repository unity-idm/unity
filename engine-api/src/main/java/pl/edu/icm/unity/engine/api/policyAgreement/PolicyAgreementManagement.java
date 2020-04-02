/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyAgreement;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Policy agreements management API
 * 
 * @author P.Piernik
 *
 */
public interface PolicyAgreementManagement
{
	List<PolicyAgreementConfiguration> filterAgreementToPresent(EntityParam entity,
			List<PolicyAgreementConfiguration> toFilter) throws EngineException;

	void submitDecisions(EntityParam entity, List<PolicyAgreementDecision> decisions) throws EngineException;
}