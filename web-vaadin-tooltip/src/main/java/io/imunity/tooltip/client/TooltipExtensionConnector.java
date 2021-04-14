package io.imunity.tooltip.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import io.imunity.tooltip.TooltipExtension;

@Connect(TooltipExtension.class)
public class TooltipExtensionConnector extends AbstractExtensionConnector
{
	private Widget baseWidget;
	private String tooltipText;
	private String vaadinIconHtml;
	private String baseElementWidth;

	public TooltipExtensionConnector()
	{
	}

	@Override
	public TooltipExtensionState getState()
	{
		return (TooltipExtensionState) super.getState();
	}

	@Override
	protected void extend(ServerConnector target)
	{
		tooltipText = getState().tooltipText;
		vaadinIconHtml = getState().vaadinIconHtml;
		baseElementWidth = getState().baseElementWidth;
		if (baseWidget == null)
		{
			baseWidget = ((AbstractComponentConnector) target).getWidget();
			if (baseWidget.isAttached())
			{
				handleAttach();
			}
			baseWidget.addAttachHandler(this::onAttachOrDetach);
		}
	}

	private void onAttachOrDetach(AttachEvent event)
	{
		if (event.isAttached())
		{
			handleAttach();
		}
	}

	private void handleAttach()
	{
		Element baseWidgetElement = baseWidget.getElement();
		Element table = DOM.createTable();
		Element parentElement = baseWidgetElement.getParentElement();
		parentElement.replaceChild(table, baseWidgetElement);
		
		Element tdWithBaseWidget = DOM.createTD();
		tdWithBaseWidget.appendChild(baseWidgetElement);
		tdWithBaseWidget.getStyle().setProperty("width", baseElementWidth);
		
		String containerId = DOM.createUniqueId();
		Element tooltipIcon = DOM.createDiv();
		tooltipIcon.setInnerHTML(vaadinIconHtml);
		tooltipIcon.setClassName("icon-container");
		tooltipIcon.setId(containerId);
		Element tdWithTooltip = DOM.createTD();
		tdWithTooltip.appendChild(tooltipIcon);
		
		Element tableRow = DOM.createTR();
		tableRow.appendChild(tdWithBaseWidget);
		tableRow.appendChild(tdWithTooltip);
		
		table.setClassName("tooltipContentTable");
		table.appendChild(tableRow);
		table.setAttribute("cellspacing", "0");
		
		attachTooltip(containerId, tooltipText);
	}
	
	private native void attachTooltip(String targetId, String tooltipContent) 
	/*-{
		$wnd.tippy($wnd.document.getElementById(targetId), {
		    placement: 'right',
		    allowHTML: true,
		    content: tooltipContent,
			zIndex: 10010
		});
	}-*/;
}
