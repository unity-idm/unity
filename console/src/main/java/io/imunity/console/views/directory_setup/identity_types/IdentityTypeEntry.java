/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.identity_types;

import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;

/**
 * Represent grid identity type entry.
 * 
 * @author P.Piernik
 *
 */
record IdentityTypeEntry (IdentityType type, IdentityTypeDefinition typeDefinition)
{
}
