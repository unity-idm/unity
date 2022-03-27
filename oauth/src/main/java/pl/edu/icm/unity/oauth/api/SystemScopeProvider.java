/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.api;

import java.util.List;

public interface SystemScopeProvider
{
	List<Scope> getScopes();
	String getId();
}
