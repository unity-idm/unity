/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;


import com.vaadin.flow.function.SerializablePredicate;

interface EntityFilter extends SerializablePredicate<IdentityEntry>
{

}
