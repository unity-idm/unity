/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.json.JsonUtil;

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
		Resource[] resources = getResources(path + "/*.json");

		if (resources == null || resources.length == 0)
		{
			return jsons;
		}
		
		try
		{	
			for (Resource r : resources)
			{
				ObjectNode json;
				String source = IOUtils.toString(r.getInputStream(), StandardCharsets.UTF_8);
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
	
	public Collection<NamedJson> readNamedJsons(String path) throws EngineException
	{
		ArrayList<NamedJson> jsons = new ArrayList<>();
		Resource[] resources = getResources(path + "/*.json");

		if (resources == null || resources.length == 0)
		{
			return jsons;
		}

		try
		{
			for (Resource r : resources)
			{
				String source = IOUtils.toString(r.getInputStream(), StandardCharsets.UTF_8);
				jsons.add(new NamedJson(r.getFilename(), JsonUtil.parse(source)));
			}

		} catch (Exception e)
		{
			throw new InternalException("Can't load json files from classpath: " + path,
					e);
		}
		return jsons;
	}

	public static class NamedJson
	{
		public final String filename;
		public final ObjectNode json;

		NamedJson(String filename, ObjectNode json)
		{
			this.filename = filename;
			this.json = json;
		}
	}

	public List<Resource> getResourcesFromClasspath(String path)
	{
		Resource[] resources = getResources(path + "/*.json");
		if (resources == null || resources.length == 0)
		{
			return new ArrayList<>();
		}
		return Stream.of(resources).collect(Collectors.toList());
	}
	
	private Resource[]  getResources(String path)
	{
		Resource[] resources = null;
		try
		{
			resources = appContext.getResources("classpath*:" + path);
		} catch (Exception e)
		{
			// empty path
		}
		return resources;	
	}
}
