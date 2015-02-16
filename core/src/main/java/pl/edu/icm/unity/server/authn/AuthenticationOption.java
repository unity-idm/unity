/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.Set;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

/**
 * Stores information about a single authentication option, as configured by administrator and selectable by 
 * a user or client. The option contains a primary authenticator and optionally advanced settings related to 
 * MFA or RBA.
 * <p>
 * This class is a working instance of what can be described by the {@link AuthenticationOptionDescription}.
 * <p>
 * Implementation note: as RBA is unimplemented yet and MFA is simplistic this class doesn't hold much. In future
 * it will be extended.
 *  
 * @author K. Benedyczak
 */
public class AuthenticationOption
{
	private BindingAuthn primaryAuthenticator;
	private BindingAuthn mandatory2ndAuthenticator;

	public AuthenticationOption(BindingAuthn primaryAuthenticator, BindingAuthn mandatory2ndAuthenticator)
	{
		this.primaryAuthenticator = primaryAuthenticator;
	}

	public BindingAuthn getPrimaryAuthenticator()
	{
		return primaryAuthenticator;
	}

	/**
	 * @return 2ndary (typically 2nd factor) authenticator. Can be null if not defined.
	 */
	public BindingAuthn getMandatory2ndAuthenticator()
	{
		return mandatory2ndAuthenticator;
	}
	
	/**
	 * @throws WrongArgumentException 
	 * 
	 */
	public void checkIfAuthenticatorsAreAmongSupported(Set<String> supportedBindings) throws WrongArgumentException
	{
		checkIfAuthenticatorIsAmongSupported(primaryAuthenticator, supportedBindings);
		if (mandatory2ndAuthenticator != null)
			checkIfAuthenticatorIsAmongSupported(mandatory2ndAuthenticator, supportedBindings);
	}

	private void checkIfAuthenticatorIsAmongSupported(BindingAuthn authenticator, 
			Set<String> supportedBindings) throws WrongArgumentException
	{
		if (!supportedBindings.contains(authenticator.getBindingName()))
				throw new WrongArgumentException("The authenticator of type " + 
						authenticator.getBindingName() + 
						" is not supported by the binding. Supported are: " + 
						supportedBindings); 
	}
}
