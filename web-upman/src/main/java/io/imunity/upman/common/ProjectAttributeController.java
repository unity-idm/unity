/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.upman.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Retrieves project attribute names 
 * @author P.Piernik
 *
 */
@Component
public class ProjectAttributeController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, ProjectAttributeController.class);
	
	private DelegatedGroupManagement delGroupMan;
	private UnityMessageSource msg;
	

	@Autowired
	public ProjectAttributeController(DelegatedGroupManagement delGroupMan, UnityMessageSource msg)
	{
		super();
		this.delGroupMan = delGroupMan;
		this.msg = msg;
	}

	/**
	 * 
	 * @param projectPath
	 * @return map which attribute name as key and attribute displayed name as value
	 * @throws ControllerException
	 */
	public Map<String, String> getAdditionalAttributeNamesForProject(String projectPath) throws ControllerException
	{

		Map<String, String> attrs = new LinkedHashMap<>();
		try
		{
			DelegatedGroup group = delGroupMan.getContents(projectPath, projectPath).group;
			if (group == null)
				return attrs;

			List<String> groupAttrs = group.delegationConfiguration.attributes;

			if (groupAttrs == null || groupAttrs.isEmpty())
				return attrs;

			for (String attr : groupAttrs)
			{
				attrs.put(attr, delGroupMan.getAttributeDisplayedName(projectPath, attr));
			}
		} catch (Exception e)
		{
			log.debug("Can not get attribute names for project " + projectPath, e);
			throw new ServerFaultException(msg);
		}
		return attrs;
	}
}
