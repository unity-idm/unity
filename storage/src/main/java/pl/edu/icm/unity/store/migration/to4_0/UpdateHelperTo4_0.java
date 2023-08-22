/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_0;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class UpdateHelperTo4_0
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperTo4_0.class);

	public static Optional<TextNode> updateHomeUIConfiguration(ObjectNode node)
	{
		if (node.get("typeId").textValue().equals("UserHomeUI"))
		{
			String originalConfiguration = node.get("configuration").get("configuration").textValue();
			String configuration = originalConfiguration;
			String newLine = System.lineSeparator();
			List<String> disabledComponents = Arrays.stream(configuration.split(newLine))
					.filter(line -> line.contains("unity.userhome.disabledComponents"))
					.toList();
			for (String disabledRow : disabledComponents)
			{
				if (disabledRow.contains("userInfo") || disabledRow.contains("identitiesManagement"))
				{
					configuration = configuration.replace(disabledRow + newLine, "");
					if (!disabledRow.startsWith("#"))
						log.info("This row {} has been removed from endpoint configuration {}", disabledRow, node.get("name").textValue());
				}
				if (disabledRow.contains("credentialTab") && !disabledRow.startsWith("#"))
				{
					String trustedDevice = "unity.userhome.disabledComponents.10=trustedDevices";
					configuration += trustedDevice + newLine;
					log.info("This row {} has been add to endpoint configuration {}", trustedDevice, node.get("name").textValue());
				}
			}
			if(!originalConfiguration.equals(configuration))
				return Optional.of(new TextNode(configuration));
		}
		return Optional.empty();
	}
}
