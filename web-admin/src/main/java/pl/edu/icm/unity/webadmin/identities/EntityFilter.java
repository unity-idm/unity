/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import com.vaadin.server.SerializablePredicate;

/**
 * Filter which can be applied on the {@link IdentitiesGrid}.
 * @author K. Benedyczak
 */
public interface EntityFilter extends SerializablePredicate<IdentityEntry>
{

}
