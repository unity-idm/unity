/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

/**
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

}
