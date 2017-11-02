/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.utils;

import java.util.ArrayList;
import java.util.Collection;


import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Read json files from classpath resource dir. 
 * @author P.Piernik
 *
 */
public class ClasspathResourceReader
{
	private ApplicationContext appContext;
	
	public ClasspathResourceReader(ApplicationContext appContext)
	{
		this.appContext = appContext;
	}

	public Collection<ObjectNode> readJsons(String path) throws EngineException
	{
		ArrayList<ObjectNode> jsons = new ArrayList<>();
		Resource[] resources = null;
		try
		{
			resources = appContext.getResources("classpath:" + path + "/*.json");
		} catch (Exception e)
		{
			// empty path
			return jsons;
		}

		if (resources == null || resources.length == 0)
		{
			return jsons;
		}
		
		try
		{	
			for (Resource r : resources)
			{
				ObjectNode json;
				String source = IOUtils.toString(r.getInputStream());
				json = JsonUtil.parse(source);
				jsons.add(json);
			}

		} catch (Exception e)
		{
			throw new InternalException("Can't load json files from classpath: " + path,
					e);
		}
		return jsons;
	}
}
