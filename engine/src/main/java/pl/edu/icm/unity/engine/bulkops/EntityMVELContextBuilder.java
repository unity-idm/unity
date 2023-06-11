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
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulkops.EntityMVELContextKey;

/**
 * Helper responsible for build or print entity related MVEL context
 * 
 * @author P.Piernik
 */
public class EntityMVELContextBuilder
{
	private static final Set<String> SENSITIVE = Sets.newHashSet("hash", "cred", "pass");

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

	public static Map<String, Object> getContext(EntityInGroupData membershipInfo)
	{
		return getContext(membershipInfo.entity.getIdentities(), membershipInfo.entity.getState().toString(),
				membershipInfo.entity.getCredentialInfo(), membershipInfo.groups,
				membershipInfo.groupAttributesByName.values());
	}

	public static Map<String, Object> getContext(List<Identity> identities, String entityStatus,
			CredentialInfo credentialInfo, Set<String> groups, Collection<AttributeExt> attributes)
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
		ctx.put(EntityMVELContextKey.attr.name(), attr);
		ctx.put(EntityMVELContextKey.attrs.name(), attrs);

		ctx.put(EntityMVELContextKey.groups.name(), groups);
		ctx.put(EntityMVELContextKey.idsByType.name(), idsByType);
		ctx.put(EntityMVELContextKey.idsByTypeObj.name(), idsByTypeObj);
		ctx.put(EntityMVELContextKey.status.name(), entityStatus);
		ctx.put(EntityMVELContextKey.credReq.name(), credentialInfo.getCredentialRequirementId());

		Map<String, CredentialPublicInformation> credentialsInfo = credentialInfo.getCredentialsState();
		Map<String, String> credentialsStatus = new HashMap<>();
		for (Entry<String, CredentialPublicInformation> entry : credentialsInfo.entrySet())
			credentialsStatus.put(entry.getKey(), entry.getValue().getState().name());
		ctx.put(EntityMVELContextKey.credStatus.name(), credentialsStatus);

		return ctx;
	}
}
