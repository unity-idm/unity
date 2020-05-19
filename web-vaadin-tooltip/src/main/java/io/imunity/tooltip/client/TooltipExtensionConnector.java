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
	private String tooltipText = "";

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
		attachFixedTooltip(element, fixedTooltipContent);
		attachNestedTooltip(containerId, tooltipText);
	}
	
	private String createContentOfFixedTooltip(String id)
	{
		return "<div class=\"icon-container\" id=\"" + id + "\">" + getHelpSvg() + "</div>";
	}
	
	private String getHelpSvg()
	{
		return  "<svg class=\"help-icon\" width=\"100%\" height=\"100%\" viewBox=\"0 0 16 16\" xmlns=\"http://www.w3.org/2000/svg\">" + 
				"    <path fill-rule=\"evenodd\" d=\"M16 8A8 8 0 110 8a8 8 0 0116 0zM6.57 6.033H5.25C5.22 4.147 6.68 3.5 8.006 3.5c1.397 0 2.673.73 2.673 2.24 0 1.08-.635 1.594-1.244 2.057-.737.559-1.01.768-1.01 1.486v.355H7.117l-.007-.463c-.038-.927.495-1.498 1.168-1.987.59-.444.965-.736.965-1.371 0-.825-.628-1.168-1.314-1.168-.901 0-1.358.603-1.358 1.384zm1.251 6.443c-.584 0-1.009-.394-1.009-.927 0-.552.425-.94 1.01-.94.609 0 1.028.388 1.028.94 0 .533-.42.927-1.029.927z\" clip-rule=\"evenodd\"/>" + 
				"</svg>";
	}
	
	private native void attachFixedTooltip(Element target, String fixedContent) 
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
