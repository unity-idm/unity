/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.endpoints;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.spi.IdpServiceAdditionalAction;
import io.imunity.console.views.CommonViewParam;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import jakarta.annotation.security.PermitAll;

import java.util.Optional;

@PermitAll
@Route(value = "/idpServices/additional", layout = ConsoleMenu.class)
public class AdditionalIdpServiceView extends ConsoleViewComponent
{

	private final IdpServiceAdditionalActionsRegistry extraActionsRegistry;
	private BreadCrumbParameter breadCrumbParameter;

	public AdditionalIdpServiceView(IdpServiceAdditionalActionsRegistry extraActionsRegistry)
	{
		this.extraActionsRegistry = extraActionsRegistry;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String serviceName)
	{
		getContent().removeAll();
		String name = Optional.ofNullable(event.getLocation().getQueryParameters()
				.getParameters().getOrDefault(CommonViewParam.name.name(), null))
				.map(l -> l.get(0))
				.orElse(null);
		IdpServiceAdditionalAction additionAction = extraActionsRegistry.getByName(name);
		breadCrumbParameter = new BreadCrumbParameter(serviceName, serviceName, additionAction.getDisplayedName(), true);
		getContent().add(additionAction.getActionContent(serviceName));
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}
}
