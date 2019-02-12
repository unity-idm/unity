/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.bulkops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.bulk.GroupMembershipInfo;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Helper responsible for build or print entity related MVEL context
 * 
 * @author P.Piernik
 *
 */

public class EntityMVELContextBuilder
{
	private static final Set<String> SENSITIVE = Sets.newHashSet("hash", "cred", "pass");

	public enum ContextKey
	{
		idsByType, idsByTypeObj, attrs, attr, groups, status, credReq
	}

	public static String ctx2ReadableString(Object context, String pfx)
	{
		if (!(context instanceof Map))
			return context.toString();
		Map<?, ?> map = (Map<?, ?>) context;
		StringBuilder ret = new StringBuilder(10240);
		map.forEach((k, v) -> {
			String key = k.toString();
			ret.append(pfx).append(k).append(": ");
			if (seemsSensitive(key))
				ret.append("--MASKED--").append("\n");
			else
				ret.append(ctx2ReadableString(v, pfx + "  ")).append("\n");
		});

		return ret.toString();
	}

	private static boolean seemsSensitive(String key)
	{
		for (String checked : SENSITIVE)
			if (key.contains(checked))
				return true;
		return false;
	}

	public static Map<String, Object> getContext(GroupMembershipInfo membershipInfo)
	{
		return getContext(membershipInfo.identities, membershipInfo.entityInfo.getEntityState().toString(),
				membershipInfo.credentialInfo.getCredentialRequirementId(), membershipInfo.groups,
				membershipInfo.attributes.get("/").values());
	}

	public static Map<String, Object> getContext(List<Identity> identities, String entityStatus,
			String credentialReq, Set<String> groups, Collection<AttributeExt> attributes)
	{
		Map<String, Object> ctx = new HashMap<>();

		Map<String, List<String>> idsByType = new HashMap<>();
		Map<String, List<Object>> idsByTypeObj = new HashMap<>();
		for (Identity identity : identities)
		{
			List<String> vals = idsByType.get(identity.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<>();
				idsByType.put(identity.getTypeId(), vals);
			}
			vals.add(identity.getValue());

			List<Object> valsObj = idsByTypeObj.get(identity.getTypeId());
			if (valsObj == null)
			{
				valsObj = new ArrayList<>();
				idsByTypeObj.put(identity.getTypeId(), valsObj);
			}
			valsObj.add(identity.getValue());
		}

		Map<String, Object> attr = new HashMap<>();
		Map<String, List<String>> attrs = new HashMap<>();

		for (AttributeExt attribute : attributes)
		{
			Object v = attribute.getValues().isEmpty() ? "" : attribute.getValues().get(0);
			attr.put(attribute.getName(), v);
			attrs.put(attribute.getName(), attribute.getValues());
		}
		ctx.put(ContextKey.attr.name(), attr);
		ctx.put(ContextKey.attrs.name(), attrs);

		ctx.put(ContextKey.groups.name(), groups);
		ctx.put(ContextKey.idsByType.name(), idsByType);
		ctx.put(ContextKey.idsByTypeObj.name(), idsByTypeObj);
		ctx.put(ContextKey.status.name(), entityStatus);
		ctx.put(ContextKey.credReq.name(), credentialReq);

		return ctx;
	}
}
