/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Controller for project management
 * 
 * @author P.Piernik
 *
 */
@Component
public class ProjectController
{
	
	
	//TODO
	Map<String, String> getProjectForUser(long entityId)
	{
		Map<String, String> projects = new HashMap<>();

		projects.put("/A", "A");
		projects.put("/unicore", "UNICORE");
		projects.put("/", "ROOT");

		return projects;

	}
}
