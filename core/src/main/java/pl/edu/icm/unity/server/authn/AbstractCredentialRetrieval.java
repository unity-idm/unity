/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;


/**
 * Common code for all {@link CredentialRetrieval} implementations. Stores the exchange implementation and instance
 * name. Useful for all implementations supporting a single type of credential exchange 
 * (what is by far the common case).
 * @author K. Benedyczak
 */
public abstract class AbstractCredentialRetrieval<T extends CredentialExchange> implements CredentialRetrieval
{
	protected T credentialExchange;
	private String authenticatorId;
	protected final String bindingName;
	
	public AbstractCredentialRetrieval(String bindingName)
	{
		this.bindingName = bindingName;
	}

	public String getBindingName()
	{
		return bindingName;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setCredentialExchange(CredentialExchange e, String id)
	{
		this.credentialExchange = (T)e;
		this.authenticatorId = id;
	}
	
	@Override
	public String getAuthenticatorId()
	{
		return authenticatorId;
	}
}
