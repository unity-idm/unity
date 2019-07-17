/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.endpoints.authnlayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.endpoints.layout.elements.ExpandColumnElement;
import pl.edu.icm.unity.webui.authn.endpoints.layout.elements.LastUsedOptionColumnElement;
import pl.edu.icm.unity.webui.authn.endpoints.layout.elements.SeparatorColumnElement;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * 
 * @author P.Piernik
 *
 */
public class WebServiceReturningLayoutEditor extends CustomComponent
{
	private UnityMessageSource msg;
	private HorizontalLayout columnLayout;
	private LayoutColumn column;
	private Runnable dragStart = () -> column.dragOn();
	private Runnable dragStop = () -> column.dragOff();
	private Consumer<ColumnElement> removeElementListener = e -> column.removeElement(e);
	private VerticalLayout main;

	public WebServiceReturningLayoutEditor(VaadinEndpointProperties properties, UnityMessageSource msg)
	{
		this.msg = msg;
		initUI();
		column.setElements(AuthnLayoutPropertiesHelper.getReturingUserColumnElements(properties, msg,
				removeElementListener, dragStart, dragStop));

		if (column.getElements().isEmpty())
		{
			column.setElements(new ArrayList<>(
					Arrays.asList(new LastUsedOptionColumnElement(msg, null, dragStart, dragStop),
							new ExpandColumnElement(msg, null, dragStart, dragStop))));
		}

	}

	private void initUI()
	{

		main = new VerticalLayout();
		main.setMargin(false);
		main.setWidth(100, Unit.PERCENTAGE);

		main.addComponent(new Label(msg.getMessage("WebServiceAuthnScreenLayoutEditor.dragElement")));
		main.addComponent(getPallete());
		main.addComponent(HtmlTag.horizontalLine());

		columnLayout = new HorizontalLayout();
		columnLayout.setWidth(50, Unit.PERCENTAGE);
		column = new LayoutColumn(msg, null, e -> column.removeElement(e), () -> column.dragOn(),
				() -> column.dragOff());
		column.setRemoveVisible(false);
		columnLayout.addComponent(column);

		main.addComponent(columnLayout);

		setCompositionRoot(main);
	}

	private HorizontalLayout getPallete()
	{
		HorizontalLayout componentsPalette = new HorizontalLayout();
		componentsPalette.addComponent(new PalleteButton(msg.getMessage("AuthnColumnLayoutElement.separator"),
				Images.text.getResource(), dragStart, dragStop,
				() -> new SeparatorColumnElement(msg, removeElementListener, dragStart, dragStop)));
		return componentsPalette;
	}

	public Properties getConfiguration() throws FormValidationException
	{

		Properties raw = new Properties();
		String content = AuthnLayoutPropertiesHelper.getColumnContent(msg, column, "RETUSER", raw);
		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT,
				content);
		return raw;
	}

}
