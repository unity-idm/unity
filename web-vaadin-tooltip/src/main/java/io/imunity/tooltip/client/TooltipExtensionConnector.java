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
	private Integer topOffset;
	private String vaadinIconHtml;

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
		topOffset = getState().topOffset;
		vaadinIconHtml = getState().vaadinIconHtml;
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
		Element element = baseWidget.getElement();
		String containerId = DOM.createUniqueId();
		String fixedTooltipContent = createContentOfFixedTooltip(containerId);
		attachFixedTooltip(element, topOffset, fixedTooltipContent);
		attachNestedTooltip(containerId, tooltipText);
	}
	
	private String createContentOfFixedTooltip(String id)
	{
		return "<div class=\"icon-container\" id=\"" + id + "\">" + vaadinIconHtml + "</div>";
	}
	
	private native void attachFixedTooltip(Element target, int offset, String fixedContent) 
	/*-{
		$wnd.tippy(target, {
		    arrow: false,
		    theme: 'help',
		    placement: 'right',
		    allowHTML: true,
		    showOnCreate: true,
		    hideOnClick: false,
		    trigger: 'manual',
		    interactive: true,
		    offset: [offset, -10],
		    popperOptions: {
		        modifiers: [
		            {
						name: 'flip',
						options: {
							fallbackPlacements: []
						}
		            }
		        ]
		    },
		    content: fixedContent
		});
	}-*/;
	
	private native void attachNestedTooltip(String targetId, String tooltipContent) 
	/*-{
		$wnd.tippy($wnd.document.getElementById(targetId), {
		    placement: 'right',
		    allowHTML: true,
		    trigger: 'click',
		    content: tooltipContent
		});
	}-*/;
}
