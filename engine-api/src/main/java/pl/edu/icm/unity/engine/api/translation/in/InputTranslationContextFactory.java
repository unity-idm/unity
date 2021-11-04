/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;

public class InputTranslationContextFactory
{
	public static Map<String, Object> createMvelContext(RemotelyAuthenticatedInput input)
	{
		Map<String, Object> ret = new HashMap<>();
		
		ret.put(InputTranslationMVELContextKey.idp.name(), input.getIdpName());
		Map<String, Object> attr = new HashMap<>();
		Map<String, List<Object>> attrs = new HashMap<>();
		for (RemoteAttribute ra: input.getAttributes().values())
		{
			Object v = ra.getValues().isEmpty() ? "" : ra.getValues().get(0);
			attr.put(ra.getName(), v);
			attrs.put(ra.getName(), ra.getValues());
		}
		ret.put(InputTranslationMVELContextKey.attr.name(), attr);
		ret.put(InputTranslationMVELContextKey.attrs.name(), attrs);
		ret.put(InputTranslationMVELContextKey.attrObj.name(), input.getRawAttributes());
		
		if (!input.getIdentities().isEmpty())
		{
			RemoteIdentity ri = input.getIdentities().values().iterator().next();
			ret.put(InputTranslationMVELContextKey.id.name(), ri.getName());
			ret.put(InputTranslationMVELContextKey.idType.name(), ri.getIdentityType());
		} else
		{
			ret.put(InputTranslationMVELContextKey.id.name(), null);
			ret.put(InputTranslationMVELContextKey.idType.name(), null);
		}
		
		Map<String, List<String>> idsByType = new HashMap<String, List<String>>();
		for (RemoteIdentity ri: input.getIdentities().values())
		{
			List<String> vals = idsByType.get(ri.getIdentityType());
			if (vals == null)
			{
				vals = new ArrayList<String>();
				idsByType.put(ri.getIdentityType(), vals);
			}
			vals.add(ri.getName());
		}
		ret.put(InputTranslationMVELContextKey.idsByType.name(), idsByType);
		
		ret.put(InputTranslationMVELContextKey.groups.name(), new ArrayList<String>(input.getGroups().keySet()));
		return ret;
	}
	
	public static Map<String, String> createExpresionValueMap(RemotelyAuthenticatedInput input)
	{
		Map<String, Object> mvelCtx = createMvelContext(input);
		return createExpresionValueMap(mvelCtx);
	}
	
	private static Map<String, String> createExpresionValueMap(Map<String, Object> mvelCtx)
	{
		Map<String, String> exprValMap = new LinkedHashMap<>();

		for (Map.Entry<String, Object> context : mvelCtx.entrySet())
		{
			String contextKey = context.getKey();
			Object contextValue = context.getValue();
			try
			{
				InputTranslationMVELContextKey.valueOf(contextKey);
			} catch (Exception e)
			{
				throw new IllegalArgumentException("Incorrect MVEL context, unknown context key: " + 
						context.getKey());
			}
			if (InputTranslationMVELContextKey.valueOf(contextKey) == InputTranslationMVELContextKey.attrObj)
				continue;
			
			if (contextValue instanceof Map)
			{
				@SuppressWarnings("unchecked")
				HashMap<String, Object> value = (HashMap<String, Object>) contextValue;
				for (Map.Entry<String, Object> entry : value.entrySet())
				{
					if (entry.getValue()  == null) 
					{
						continue;
					}
					exprValMap.put(String.format("%s['%s']", contextKey, entry.getKey()), 
							entry.getValue().toString());
				}
			} else if (contextValue instanceof List)
			{
				exprValMap.put(contextKey, contextValue.toString());
				
			} else if (contextValue instanceof String)
			{
				exprValMap.put(contextKey, contextValue.toString());
				
			} else
			{
				throw new IllegalArgumentException("Incorrect MVEL context: unexpected: \"" 
						+ contextValue.getClass() 
						+ "\" type for context key: \"" 
						+ contextKey 
						+ "\"");
			}
		}
		
		return exprValMap;
	}

}
