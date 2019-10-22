/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

/**
 * Contains information about db dump content. 
 * 
 * @author P.Piernik
 *
 */
public class DBDumpContentType
{
	private boolean systemConfig = true;
	private boolean directorySchema = true;
	private boolean users = true;
	private boolean auditLogs = true;
	private boolean signupRequests = true;

	public DBDumpContentType()
	{

	}

	public boolean isSystemConfig()
	{
		return systemConfig;
	}

	public void setSystemConfig(boolean systemConfig)
	{
		this.systemConfig = systemConfig;
	}

	public boolean isDirectorySchema()
	{
		return directorySchema;
	}

	public void setDirectorySchema(boolean directorySchema)
	{
		this.directorySchema = directorySchema;
	}

	public boolean isUsers()
	{
		return users;
	}

	public void setUsers(boolean users)
	{
		this.users = users;
	}

	public boolean isAuditLogs()
	{
		return auditLogs;
	}

	public void setAuditLogs(boolean auditLogs)
	{
		this.auditLogs = auditLogs;
	}

	public boolean isSignupRequests()
	{
		return signupRequests;
	}

	public void setSignupRequests(boolean signupRequests)
	{
		this.signupRequests = signupRequests;
	}
}
