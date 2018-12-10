/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.time.Instant;
import java.util.List;

public class ProjectRequest
{
	public static enum RequestOperation
	{
		Update, SelfSignUp
	};

	public final String id;
	public final RequestOperation operation;
	public final String project;
	public final String name;
	public final String email;
	public final List<String> groups;
	public final Instant requestedTime;

	public ProjectRequest(String id, RequestOperation operation, String project, String name, String email,
			List<String> groups, Instant requestedTime)
	{
		this.id = id;
		this.operation = operation;
		this.project = project;
		this.name = name;
		this.email = email;
		this.groups = groups;
		this.requestedTime = requestedTime;
	}
}
