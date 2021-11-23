/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.sandbox;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;

public class SandboxAuthenticationResult implements AuthenticationResult
{
	public final SandboxAuthnContext sandboxAuthnInfo;
	private final AuthenticationResult baseResult;

	public SandboxAuthenticationResult(AuthenticationResult base, SandboxAuthnContext sandboxAuthnInfo)
	{
		this.baseResult = base;
		this.sandboxAuthnInfo = sandboxAuthnInfo;
	}

	/**
	 * We have two cases (what is sort of design flaw): we can get sandbox result directly (a typical case) 
	 * or we can get LocalAuthnResult, in case when we have sandbox authn in account association flow, and 
	 * user needs to provide existing account to associate unknwon remote with it. 
	 */
	public static SandboxAuthenticationResult getInstanceFromResult(AuthenticationResult result)
	{
		if (result instanceof SandboxAuthenticationResult)
			return (SandboxAuthenticationResult) result;
		return new SandboxAuthenticationResult(result, new EmptySandboxAuthnConext());
	}
	
	@Override
	public Status getStatus()
	{
		return baseResult.getStatus();
	}

	@Override
	public boolean isRemote()
	{
		return baseResult.isRemote();
	}

	@Override
	public String toStringFull()
	{
		return String.format("SandboxAuthenticationResult [sandboxAuthnInfo=%s, baseResult=%s]",
				sandboxAuthnInfo, baseResult.toString());
	}

	@Override
	public SuccessResult getSuccessResult()
	{
		return baseResult.getSuccessResult();
	}

	@Override
	public ErrorResult getErrorResult()
	{
		return baseResult.getErrorResult();
	}

	@Override
	public String toString()
	{
		return baseResult.toString();
	}
	
	@Override
	public RemoteAuthenticationResult asRemote()
	{
		return baseResult.asRemote();
	}
	
	
}
