/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyDocument;

public class PolicyDocumentNotFoundException extends RuntimeException
{
	public PolicyDocumentNotFoundException(String message)
	{
		super(message);
	}
}
