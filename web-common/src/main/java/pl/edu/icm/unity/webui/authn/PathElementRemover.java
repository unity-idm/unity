/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn;

import java.util.Set;

class PathElementRemover
{
	static String removePathElementsUntil(String path, Set<String> until)
	{
		boolean adding = false;
		StringBuilder newPath = new StringBuilder();
		for(String element : path.split("/"))
		{
			if(until.contains(element))
				adding = true;
			if(adding)
				newPath.append("/").append(element);
		}
		return newPath.toString();
	}
}
