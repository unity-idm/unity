/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Trivial bean providing a list of Strings, being message bundle names.
 * Unity modules define beans of this class. All such beans are used by the 
 * {@link UnityMessageSource} to build a composite, master message source.
 * @author K. Benedyczak
 */
@Component
public class UnityMessageBundles
{
	private List<String> bundles = new ArrayList<String>();

	public List<String> getBundles()
	{
		return bundles;
	}

	@Autowired(required=false)
	public void setBundles(List<String> bundles)
	{
		this.bundles = bundles;
	}

	@Autowired(required=false)
	public void setBundle(String bundle)
	{
		this.bundles = Collections.singletonList(bundle);
	}
}
