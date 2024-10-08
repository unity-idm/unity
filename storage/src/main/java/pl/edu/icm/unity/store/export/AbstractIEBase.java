/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Generic base code for importers and exporters.
 * @author K. Benedyczak
 */
public abstract class AbstractIEBase<T>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, AbstractIEBase.class);
	private int sortKey;
	private String storeKey;
	protected ObjectMapper jsonMapper;
	
	public AbstractIEBase(int sortKey, String storeKey, ObjectMapper objectMapper)
	{
		this.sortKey = sortKey;
		this.storeKey = storeKey;
		this.jsonMapper = objectMapper;
	}

	/**
	 * @return all objects to be exported
	 */
	protected abstract List<T> getAllToExport();
	/**
	 * Converts in-db object to JSON form
	 * @param exportedObj
	 * @return
	 */
	protected abstract ObjectNode toJsonSingle(T exportedObj);

	/**
	 * Converts imported object from JSON to the in-db form
	 * @param src
	 * @return
	 */
	protected abstract T fromJsonSingle(ObjectNode src);

	/**
	 * Stores in imported object in db
	 * @param toCreate
	 */
	protected abstract void createSingle(T toCreate);
	
	protected List<T> sortBeforeImport(List<T> toSort)
	{
		return toSort;
	}
	
	public int getSortKey()
	{
		return sortKey;
	}

	public String getStoreKey()
	{
		return storeKey;
	}

	public void serialize(JsonGenerator jg) throws IOException
	{
		List<T> all = getAllToExport();
		jg.writeStartArray();
		for (T obj: all)
			serializeToJson(jg, obj);
		jg.writeEndArray();
	}
	
	protected void serializeToJson(JsonGenerator jg, T obj) throws IOException
	{
		ObjectNode asJson = toJsonSingle(obj);
		jg.writeTree(asJson);
	}
	
	public void deserialize(JsonParser input) 
			throws IOException
	{
		JsonUtils.expect(input, JsonToken.START_ARRAY);
		
		List<T> toImportElements = new ArrayList<T>();
		while(input.nextToken() == JsonToken.START_OBJECT)
		{
			T obj = deserializeFromJson(input);
			if (obj != null)
			{
				toImportElements.add(obj);
			}
		}
		List<T> toImportElementsSorted = sortBeforeImport(toImportElements);
		for (T e: toImportElementsSorted)
		{
			createSingle(e);
		}
		
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
	
	private T deserializeFromJson(JsonParser input)
			throws IOException
	{
		ObjectNode read = input.readValueAsTree();
		try
		{
			return fromJsonSingle(read);
		} catch (Exception e)
		{
			log.error("Loading dump failed at reading JSON element: " + read);
			throw e;
		}
	}
}



