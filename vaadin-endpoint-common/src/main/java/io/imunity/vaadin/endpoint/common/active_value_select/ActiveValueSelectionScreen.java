/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.active_value_select;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.MessageSource;
import io.imunity.vaadin.endpoint.common.consent_utils.IdPButtonsBar;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;


public class ActiveValueSelectionScreen extends VerticalLayout
{
	private final AttributeProcessor attrProcessor;
	private final MessageSource msg;
	private final StandardWebLogoutHandler authnProcessor;

	private Map<DynamicAttribute, ValueSelector> selectors;
	private final Runnable declineHandler;
	private final Consumer<List<DynamicAttribute>> acceptHandler;
	private final List<DynamicAttribute> remainingAttributes;
	
	public ActiveValueSelectionScreen(MessageSource msg, AttributeHandlerRegistry attrHandlerRegistry,
			StandardWebLogoutHandler authnProcessor,
			List<DynamicAttribute> singleSelectable,
			List<DynamicAttribute> multiSelectable,
			List<DynamicAttribute> remainingAttributes,
			Runnable declineHandler,
			Consumer<List<DynamicAttribute>> acceptHandler)
	{
		this.msg = msg;
		this.remainingAttributes = remainingAttributes;
		this.declineHandler = declineHandler;
		this.acceptHandler = acceptHandler;
		this.attrProcessor = new AttributeProcessor(attrHandlerRegistry);
		this.authnProcessor = authnProcessor;
		initUI(singleSelectable, multiSelectable);
	}

	private void initUI(List<DynamicAttribute> singleSelectable, List<DynamicAttribute> multiSelectable)
	{
		H3 title = new H3(msg.getMessage("ActiveValueSelectionScreen.title"));
		add(title);

		VerticalLayout centered = new VerticalLayout();
		centered.setSizeUndefined();
		add(centered);

		selectors = new HashMap<>();
		for (DynamicAttribute da: singleSelectable)
		{
			if (da.getAttribute().getValues().isEmpty())
				continue;
			SingleValueSelector selector = new SingleValueSelector(da.getDisplayedName(),
					attrProcessor.getValuesForPresentation(da));
			selectors.put(da, selector);
			centered.add(selector);
		}
		
		for (DynamicAttribute da: multiSelectable)
		{
			if (da.getAttribute().getValues().isEmpty())
				continue;
			MultiValueSelector selector = new MultiValueSelector(da.getDisplayedName(),
					attrProcessor.getValuesForPresentation(da));
			selectors.put(da, selector);
			centered.add(selector);
		}
		
		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, action ->
		{
			if (action == IdPButtonsBar.Action.ACCEPT)
				acceptHandler.accept(getFilteredAttributes());
			else
				declineHandler.run();
		});
		
		add(buttons);
		setAlignItems(CENTER);
	}
	
	private List<DynamicAttribute> getFilteredAttributes()
	{
		List<DynamicAttribute> subjectToSelection = selectors.entrySet().stream()
				.map(entry -> attrProcessor.getAttributeWithActiveValues(entry.getKey(),
						entry.getValue().getSelectedValueIndices()))
				.filter(da -> !da.getAttribute().getValues().isEmpty()).toList();
		List<DynamicAttribute> ret = new ArrayList<>(remainingAttributes);
		ret.addAll(subjectToSelection);
		return ret;
	}
}
