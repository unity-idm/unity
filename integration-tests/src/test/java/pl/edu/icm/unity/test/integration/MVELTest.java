/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mvel2.MVEL;

import com.google.common.collect.Lists;

/**
 * For mvel expressions testing
 * @author K. Benedyczak
 */
public class MVELTest
{

	public static void main(String[] args)
	{
		Map<String, Object> vars = new HashMap<>();
		
		Map<String, List<String>> attrs = new HashMap<>();
		attrs.put("groupNames", Lists.newArrayList("lifescience-pilot:testGroup1", 
				"lifescience-pilot:testGroup1:testSubGroup1", 
				"lifescience-pilot:testGroup2"));
		vars.put("attrs", attrs);
		String expression = "('urn:geant:lifescienceid.org:group:' + toString() + '#perun.lifescienceid.org' in attrs['groupNames'])";
		
		
		System.out.println(MVEL.eval(expression, vars));
	}

}
