/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;

public class ClassPathResourceReader
{
	private ApplicationContext appContext;
	
	public ClassPathResourceReader(ApplicationContext appContext)
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
				String source = FileUtils.readFileToString(r.getFile());
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
