/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import java.util.Objects;

public class AuthorizationScriptBean
{
	private String scope;
	private String path;
	
	public AuthorizationScriptBean()
	{
	}
	
	public AuthorizationScriptBean(String scope, String path)
	{
		this.scope = scope;
		this.path = path;
	}
	public String getScope()
	{
		return scope;
	}
	public void setScope(String scope)
	{
		this.scope = scope;
	}
	public String getPath()
	{
		return path;
	}
	public void setPath(String path)
	{
		this.path = path;
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(path, scope);
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthorizationScriptBean other = (AuthorizationScriptBean) obj;
		return Objects.equals(path, other.path) && Objects.equals(scope, other.scope);
	}
}
