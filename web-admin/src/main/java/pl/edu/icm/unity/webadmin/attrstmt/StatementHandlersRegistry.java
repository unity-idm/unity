/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.attrstmt.AttributeStatementWebHandlerFactory.AttributeStatementComponent;


/**
 * Registry of {@link AttributeStatementWebHandlerFactory}s.
 * @author K. Benedyczak
 */
@Component
public class StatementHandlersRegistry
{
	private Map<String, AttributeStatementWebHandlerFactory> handlers;
	private UnityMessageSource msg;
	
	@Autowired
	public StatementHandlersRegistry(List<AttributeStatementWebHandlerFactory> handlers, UnityMessageSource msg)
	{
		this.msg = msg;
		this.handlers = new HashMap<String, AttributeStatementWebHandlerFactory>(handlers.size());
		for (AttributeStatementWebHandlerFactory factory: handlers)
			this.handlers.put(factory.getSupportedAttributeStatementName(), factory);
	}

	public Map<String, AttributeStatementComponent> getAvailableComponents(Collection<AttributeType> attributeTypes, 
			String group)
	{
		Map<String, AttributeStatementComponent> ret = new TreeMap<String, AttributeStatementComponent>();
		for (Map.Entry<String, AttributeStatementWebHandlerFactory> factory: handlers.entrySet())
			ret.put(msg.getMessage("AttributeStatements.stmt." + factory.getKey()), 
					factory.getValue().getEditorComponent(attributeTypes, group));
		return ret;
	}

	public AttributeStatementWebHandlerFactory getHandler(String statement) throws IllegalArgumentException
	{
		AttributeStatementWebHandlerFactory ret = handlers.get(statement);
		if (ret == null)
			throw new IllegalArgumentException("Unsupported attribtue statement name: " + statement);
		return ret;
	}
}
