package pl.edu.icm.unity.engine.api.idp;

import pl.edu.icm.unity.exceptions.AuthorizationException;

public interface IdpPolicyAgreementContentChecker {

	public boolean isPolicyUsedOnEndpoints(Long policyId) throws AuthorizationException;
	
}
