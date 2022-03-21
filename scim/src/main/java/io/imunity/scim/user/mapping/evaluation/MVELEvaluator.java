/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Identity;

@Component
class MVELEvaluator
{
	public final int COMPILED_MVEL_CACHE_TTL_IN_SECONDS = 60;

	private final AttributeValueConverter attrValueConverter;
	private final Cache<String, Serializable> compiledMvelCache;

	MVELEvaluator(AttributeValueConverter attrValueConverter)
	{
		this.attrValueConverter = attrValueConverter;
		this.compiledMvelCache = CacheBuilder.newBuilder()
				.expireAfterWrite(COMPILED_MVEL_CACHE_TTL_IN_SECONDS, TimeUnit.SECONDS).build();
	}

	Object evalMVEL(String mvel, EvaluatorContext context) throws IllegalAttributeValueException
	{
		Serializable expressionCompiled = compileMvel(mvel);
		return MVEL.executeExpression(expressionCompiled, createContext(context), new HashMap<>());
	}

	private Serializable compileMvel(String mvel)
	{
		Serializable cached = compiledMvelCache.getIfPresent(mvel);
		if (cached != null)
			return cached;
		Serializable actual = MVEL.compileExpression(mvel);
		compiledMvelCache.put(mvel, actual);
		return actual;
	}

	Object createContext(EvaluatorContext context) throws IllegalAttributeValueException
	{
		Map<String, Object> ret = new HashMap<>();
		ret.put(SCIMMvelContextKey.idsByType.toString(), createIdentityContextElement(context));
		ret.put(SCIMMvelContextKey.attrObj.toString(), createAttributeObjContextElement(context));
		ret.put(SCIMMvelContextKey.groups.toString(),
				context.user.groups.stream().map(g -> g.getPathEncoded()).collect(Collectors.toList()));
		if (context.arrayObj != null)
		{
			ret.put(SCIMMvelContextKey.arrayObj.toString(), context.arrayObj);
		}

		return ret;
	}

	private Map<String, Object> createAttributeObjContextElement(EvaluatorContext context)
			throws IllegalAttributeValueException
	{
		Map<String, Object> attrObj = new HashMap<>();
		for (Attribute ra : context.user.attributes)
		{
			attrObj.put(ra.getName(), ra.getValues().isEmpty() ? ""
					: attrValueConverter.internalValuesToObjectValues(ra.getName(), ra.getValues()));
		}
		return attrObj;
	}

	private Map<String, List<String>> createIdentityContextElement(EvaluatorContext context)
	{
		Map<String, List<String>> idsByType = new HashMap<>();
		for (Identity id : context.user.identities)
		{
			List<String> vals = idsByType.get(id.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<>();
				idsByType.put(id.getTypeId(), vals);
			}
			vals.add(id.getValue());
		}
		return idsByType;
	}
}
