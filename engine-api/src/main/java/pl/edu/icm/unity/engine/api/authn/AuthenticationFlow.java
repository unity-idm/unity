/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;

/**
 * Stores information about a authentication flow, as configured by administrator and selectable by 
 * a user or client. The flow contains a first factor authenticators, policy and optionally second factor authenticators. The policy decide how second factor
 * authenticator is enforced.
 * <p>
 * This class is a working instance of what can be described by the {@link AuthenticationFlowDefinition}.
 * <p>
 * Implementation note: as RBA is unimplemented yet. In future
 * it will be extended.
 *  
 * @author K. Benedyczak
 */
public class AuthenticationFlow
{
	private Set<BindingAuthn> firstFactorAuthenticators;
	private List<BindingAuthn> secondFactorAuthenticators;
	private Policy policy;
	private String name;

	public AuthenticationFlow(String name, Policy policy,
			Set<BindingAuthn> firstFactorAuthenticators,
			List<BindingAuthn> secondFactorAuthenticators)
	{
		this.name = name;
		this.policy = policy;
		this.firstFactorAuthenticators = firstFactorAuthenticators;
		this.secondFactorAuthenticators = secondFactorAuthenticators;
	}

	public String getId()
	{
		return name;
	}
	
	public Set<BindingAuthn> getFirstFactorAuthenticators()
	{
		return firstFactorAuthenticators;
	}

	/**
	 * @return 2ndary (typically 2nd factor) authenticator. Can be null if not defined.
	 */
	public List<BindingAuthn> getSecondFactorAuthenticators()
	{
		return secondFactorAuthenticators;
	}
	
	public Policy getPolicy()
	{
		return policy;
	}
	
	public void destroy()
	{
		for (BindingAuthn firstFactor : firstFactorAuthenticators)
			firstFactor.destroy();
		for (BindingAuthn secondFactor : secondFactorAuthenticators)
			secondFactor.destroy();		
	}
	
	public Set<BindingAuthn> getAllAuthenticators()
	{
		Set<BindingAuthn> ret = new HashSet<>();
		ret.addAll(firstFactorAuthenticators);
		ret.addAll(secondFactorAuthenticators);
		return ret;	
	}
	
	/**
	 * @throws WrongArgumentException 
	 * 
	 */
	public void checkIfAuthenticatorsAreAmongSupported(Set<String> supportedBindings) throws WrongArgumentException
	{
		checkIfAuthenticatorIsAmongSupported(firstFactorAuthenticators, supportedBindings);
		checkIfAuthenticatorIsAmongSupported(secondFactorAuthenticators, supportedBindings);
	}

	private void checkIfAuthenticatorIsAmongSupported(Collection<BindingAuthn> authenticators,
			Set<String> supportedBindings) throws WrongArgumentException
	{

		for (BindingAuthn authenticator : authenticators)
		{
			if (!supportedBindings.contains(authenticator.getBindingName()))
				throw new WrongArgumentException("The authenticator of type "
						+ authenticator.getBindingName()
						+ " is not supported by the binding. Supported are: "
						+ supportedBindings);
		}
	}
	
}
