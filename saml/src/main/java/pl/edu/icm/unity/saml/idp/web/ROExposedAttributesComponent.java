/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import io.imunity.vaadin.endpoint.common.consent_utils.ExposedAttributesComponent;
import io.imunity.vaadin.endpoint.common.consent_utils.ExposedSelectableAttributesComponent;
import pl.edu.icm.unity.webui.idpcommon.SelectableAttributesComponent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Non editable presentation of attributes, using {@link ExposedAttributesComponent} for the visualization
 * but exposing the same API as {@link ExposedSelectableAttributesComponent}, so is its drop in replacement.
 */
public class ROExposedAttributesComponent extends VerticalLayout implements SelectableAttributesComponent
{
	private final Collection<Attribute> attributes;
	
	public ROExposedAttributesComponent(MessageSource msg, IdentityTypeSupport idTypeSupport,
			Collection<DynamicAttribute> attributes, AttributeHandlerRegistry handlersRegistry,
			Optional<IdentityParam> selectedIdentity)
	{
		this.attributes = attributes.stream().map(DynamicAttribute::getAttribute).collect(Collectors.toList());
		ExposedAttributesComponent ui = new ExposedAttributesComponent(msg, idTypeSupport, handlersRegistry, 
				attributes, selectedIdentity);
		add(ui);
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
