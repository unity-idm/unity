/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities;


public interface IdentityEditorFactory
{
	String getSupportedIdentityType();
	IdentityEditor createInstance();
}
