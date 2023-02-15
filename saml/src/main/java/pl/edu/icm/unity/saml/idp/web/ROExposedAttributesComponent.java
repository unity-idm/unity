/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.idpcommon.ExposedAttributesComponent;
import pl.edu.icm.unity.webui.idpcommon.ExposedSelectableAttributesComponent;
import pl.edu.icm.unity.webui.idpcommon.SelectableAttributesComponent;

/**
 * Non editable presentation of attributes, using {@link ExposedAttributesComponent} for the visualization
 * but exposing the same API as {@link ExposedSelectableAttributesComponent}, so is its drop in replacement.
 */
public class ROExposedAttributesComponent extends CustomComponent implements SelectableAttributesComponent
{
	private final Collection<Attribute> attributes;
	
	public ROExposedAttributesComponent(MessageSource msg, IdentityTypeSupport idTypeSupport,
			Collection<DynamicAttribute> attributes, AttributeHandlerRegistryV8 handlersRegistry,
			Optional<IdentityParam> selectedIdentity)
	{
		this.attributes = attributes.stream().map(da -> da.getAttribute()).collect(Collectors.toList());
		ExposedAttributesComponent ui = new ExposedAttributesComponent(msg, idTypeSupport, handlersRegistry, 
				attributes, selectedIdentity);
		setCompositionRoot(ui);
	}

	@Override
	public Collection<Attribute> getUserFilteredAttributes()
	{
		return new ArrayList<>(attributes);
	}

	@Override
	public Map<String, Attribute> getHiddenAttributes()
	{
		return Collections.emptyMap();
	}

	@Override
	public void setInitialState(Map<String, Attribute> savedState)
	{
	}
}
