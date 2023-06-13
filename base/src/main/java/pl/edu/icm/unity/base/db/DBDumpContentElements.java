/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.db;

/**
 * Contains information about db dump content. 
 * 
 * @author P.Piernik
 *
 */
public class DBDumpContentElements
{
	public final boolean systemConfig;
	public final boolean directorySchema ;
	public final boolean users;
	public final boolean auditLogs;
	public final boolean signupRequests;
	public final boolean idpStatistics;

	
	public DBDumpContentElements()
	{
		this(true, true, true, true, true, true);
	}

	public DBDumpContentElements(boolean systemConfig, boolean directorySchema, boolean users, boolean auditLogs,
			boolean signupRequests, boolean idpStatistics)
	{
		this.systemConfig = systemConfig;
		this.directorySchema = directorySchema;
		this.users = users;
		this.auditLogs = auditLogs;
		this.signupRequests = signupRequests;
		this.idpStatistics = idpStatistics;
	}
}
