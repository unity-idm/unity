/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon.activesel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar.Action;

/**
 * Presents UI allowing for selecting active value or values for the given set of attributes.
 * 
 * @author K. Benedyczak
 */
public class ActiveValueSelectionScreen extends CustomComponent
{
	private final AttributeProcessor attrProcessor;
	private final UnityMessageSource msg;
	private final StandardWebAuthenticationProcessor authnProcessor;

	private Map<DynamicAttribute, ValueSelector> selectors;
	private Runnable declineHandler;
	private Consumer<List<DynamicAttribute>> acceptHandler;
	private List<DynamicAttribute> remainingAttributes;
	
	public ActiveValueSelectionScreen(UnityMessageSource msg, AttributeHandlerRegistry attrHandlerRegistry, 
			StandardWebAuthenticationProcessor authnProcessor,
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
		VerticalLayout main = new VerticalLayout();
		Label title = new Label(msg.getMessage("ActiveValueSelectionScreen.title"));
		title.addStyleName(Styles.textXLarge.toString());
		title.addStyleName("u-activeValueSelTitle");
		main.addComponent(title);
		main.setComponentAlignment(title, Alignment.TOP_CENTER);
		
		VerticalLayout centered = new VerticalLayout();
		centered.setWidthUndefined();
		main.addComponent(centered);
		main.setComponentAlignment(centered, Alignment.TOP_CENTER);
		
		selectors = new HashMap<>();
		for (DynamicAttribute da: singleSelectable)
		{
			if (da.getAttribute().getValues().isEmpty())
				continue;
			SingleValueSelector selector = new SingleValueSelector(da.getDisplayedName(), 
					attrProcessor.getValuesForPresentation(da));
			selectors.put(da, selector);
			centered.addComponent(selector);
		}
		
		for (DynamicAttribute da: multiSelectable)
		{
			if (da.getAttribute().getValues().isEmpty())
				continue;
			MultiValueSelector selector = new MultiValueSelector(da.getDisplayedName(), 
					attrProcessor.getValuesForPresentation(da));
			selectors.put(da, selector);
			centered.addComponent(selector);
		}
		
		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, action -> 
		{
			if (action == Action.ACCEPT)
				acceptHandler.accept(getFilteredAttributes());
			else
				declineHandler.run();
		});
		
		main.addComponent(buttons);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
		
		setCompositionRoot(main);
	}
	
	private List<DynamicAttribute> getFilteredAttributes()
	{
		List<DynamicAttribute> subjectToSelection = selectors.entrySet().stream()
			.map(entry -> attrProcessor.getAttributeWithActiveValues(entry.getKey(), 
					entry.getValue().getSelectedValueIndices()))
			.filter(da -> !da.getAttribute().getValues().isEmpty())
			.collect(Collectors.toList());
		List<DynamicAttribute> ret = new ArrayList<>(remainingAttributes);
		ret.addAll(subjectToSelection);
		return ret;
	}
}
