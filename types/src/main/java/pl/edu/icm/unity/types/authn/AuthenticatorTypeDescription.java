/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

/**
 * Describes an available authenticator. The contents is determined from implementations,
 * it is not modifiable at runtime.
 * @author K. Benedyczak
 */
public class AuthenticatorTypeDescription
{
	private String id;
	private String supportedBinding;
	private String verificationMethod;
	private String verificationMethodDescription;
	private String retrievalMethod;
	private String retrievalMethodDescription;
	
	
	
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public String getSupportedBinding()
	{
		return supportedBinding;
	}
	public void setSupportedBinding(String supportedBinding)
	{
		this.supportedBinding = supportedBinding;
	}
	public String getVerificationMethod()
	{
		return verificationMethod;
	}
	public void setVerificationMethod(String verificationMethod)
	{
		this.verificationMethod = verificationMethod;
	}
	public String getVerificationMethodDescription()
	{
		return verificationMethodDescription;
	}
	public void setVerificationMethodDescription(String verificationMethodDescription)
	{
		this.verificationMethodDescription = verificationMethodDescription;
	}
	public String getRetrievalMethod()
	{
		return retrievalMethod;
	}
	public void setRetrievalMethod(String retrievalMethod)
	{
		this.retrievalMethod = retrievalMethod;
	}
	public String getRetrievalMethodDescription()
	{
		return retrievalMethodDescription;
	}
	public void setRetrievalMethodDescription(String retrievalMethodDescription)
	{
		this.retrievalMethodDescription = retrievalMethodDescription;
	}
}
