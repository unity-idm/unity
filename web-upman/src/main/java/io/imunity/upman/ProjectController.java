/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for project management
 * 
 * @author P.Piernik
 *
 */
@Component
public class ProjectController
{
	private UnityMessageSource msg;

	@Autowired
	public ProjectController(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	// TODO
	Map<String, String> getProjectForUser(long entityId) throws ControllerException
	{
		Map<String, String> projects = new HashMap<>();

		 projects.put("/A", "A");
		 projects.put("/unicore", "UNICORE");
		 projects.put("/", "ROOT");

		if (projects.isEmpty())
			throw new ControllerException(
					msg.getMessage("ProjectController.noProjectAvailable"),
					new Throwable());

		return projects;

	}
}
