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

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.mvel.MVELGroup;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Identity;

@Component
class MVELEvaluator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, MVELEvaluator.class);
	
	public final int COMPILED_MVEL_CACHE_TTL_IN_HOURS = 1;

	private final AttributeValueConverter attrValueConverter;
	private final Cache<String, Serializable> compiledMvelCache;

	MVELEvaluator(AttributeValueConverter attrValueConverter)
	{
		this.attrValueConverter = attrValueConverter;
		this.compiledMvelCache = CacheBuilder.newBuilder()
				.expireAfterAccess(COMPILED_MVEL_CACHE_TTL_IN_HOURS, TimeUnit.HOURS).build();
	}

	Object evalMVEL(String mvel, EvaluatorContext context) throws IllegalAttributeValueException
	{
		if (mvel == null)
			return null;

		Serializable expressionCompiled = getCompiledMvel(mvel);
		return MVEL.executeExpression(expressionCompiled, createContext(context), new HashMap<>());
	}

	private Serializable getCompiledMvel(String mvel)
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
		addAttributesToContext(SCIMMvelContextKey.attr.name(),
				SCIMMvelContextKey.attrObj.name(), SCIMMvelContextKey.attrs.name(), ret,
				context.user.attributes, attrValueConverter);

		List<String> groups = new ArrayList<>();
		Map<String, MVELGroup> groupsObj = new HashMap<>();

		context.user.groups.forEach(g ->
		{
			groups.add(g.getPathEncoded());
			groupsObj.put(g.getName(), context.groupProvider.get(g.getPathEncoded()));
		});

		ret.put(SCIMMvelContextKey.groups.toString(), groups);
		ret.put(SCIMMvelContextKey.groupsObj.name(), groupsObj);

		if (context.arrayObj != null)
		{
			ret.put(SCIMMvelContextKey.arrayObj.toString(), context.arrayObj);
		}

		log.info("MVEL evaluation context: {}", ret);
		return ret;
	}

	private void addAttributesToContext(String attrKey, String attrObjKey, String attrsKey, Map<String, Object> ret,
			List<AttributeExt> attributes, AttributeValueConverter attrConverter) throws IllegalAttributeValueException
	{
		Map<String, Object> attr = new HashMap<>();
		Map<String, Object> attrObj = new HashMap<>();
		Map<String, List<? extends Object>> attrs = new HashMap<>();

		for (Attribute ra : attributes)
		{
			List<String> values = attrConverter.internalValuesToExternal(ra.getName(), ra.getValues());
			String v = values.isEmpty() ? "" : values.get(0);
			attr.put(ra.getName(), v);
			attrs.put(ra.getName(), values);
			attrObj.put(ra.getName(),
					values.isEmpty() ? "" : attrConverter.internalValuesToObjectValues(ra.getName(), ra.getValues()));
		}
		ret.put(attrKey, attr);
		ret.put(attrObjKey, attrObj);
		ret.put(attrsKey, attrs);
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
