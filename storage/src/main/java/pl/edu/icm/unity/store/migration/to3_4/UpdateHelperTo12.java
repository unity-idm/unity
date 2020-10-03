/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_4;

import com.fasterxml.jackson.databind.node.ArrayNode;

class UpdateHelperTo12
{
	static void updateValuesJson(ArrayNode valuesArray)
	{
		for (int i=0; i<valuesArray.size(); i++)
		{
			String jpegValue = valuesArray.get(i).asText();
			String updatedValue = "{\"type\":\"JPG\",\"value\":\"" + jpegValue + "\"}";
			valuesArray.remove(i);
			valuesArray.insert(i, updatedValue);
		}
	}
}
