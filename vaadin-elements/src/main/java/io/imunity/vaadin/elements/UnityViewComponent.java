/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.*;

import java.util.Optional;

public abstract class UnityViewComponent extends Composite<Div> implements HasUrlParameter<String>, HasDynamicTitle
{
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter)
	{
	}

	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.empty();
	}
}
