/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

/**
 * Helpers to operate Grid DnD more easily
 *  
 * @author K. Benedyczak
 */
public class DnDGridUtils
{
	public static String getTypedCriteriaScript(String type)
	{
		return	""
				+ "if (event.dataTransfer.types.includes('" + type + "')) {" 
				+ "    return true;"
				+ "}"
				+ "return false;";
	}
}
