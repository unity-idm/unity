/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import com.google.common.collect.Lists;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;

import java.util.*;
import java.util.stream.Collectors;

public class UserImportHelper
{
	public static List<UserImportSpec> getUserImportsLegacy(UserImportConfigs userImportConfigs,
	                                                        String identity, String type)
	{
		if (userImportConfigs.configs.isEmpty())
		{
			return userImportConfigs.skip ? Collections.emptyList()
					: Lists.newArrayList(UserImportSpec.withAllImporters(identity, type));
		} else
		{
			Map<String, String> map = new HashMap<>();
			map.put(type, identity);
			return getUserImports(userImportConfigs.configs, map);
		}
	}

	public static List<UserImportSpec> getUserImports(Set<UserImportConfig> userImportConfigs,
	                                                  Map<String, String> identitiesByType)
	{
		return userImportConfigs.stream()
				.map(config -> new UserImportSpec(config.importer, identitiesByType.get(config.type), config.type))
				.collect(Collectors.toList());
	}
}
