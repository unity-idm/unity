/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.backup_and_restore;

class BinderDBDumpContentElements
{
	private boolean systemConfig = true;
	private boolean directorySchema = true;
	private boolean users = true;
	private boolean auditLogs = true;
	private boolean signupRequests = true;
	private boolean idpStatistics = true;

	public BinderDBDumpContentElements()
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

	public boolean isIdpStatistics()
	{
		return idpStatistics;
	}

	public void setIdpStatistics(boolean idpStatistics)
	{
		this.idpStatistics = idpStatistics;
	}
}
