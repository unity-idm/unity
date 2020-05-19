package io.imunity.tooltip;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.AbstractComponent;

import io.imunity.tooltip.client.TooltipExtensionState;

@JavaScript({ "vaadin://popper-2.4.0.min.js", "vaadin://tippy-6.2.2.umd.min.js" })
@StyleSheet({ "vaadin://tippy.css" })
public class TooltipExtension extends AbstractJavaScriptExtension
{
	@Override
	protected TooltipExtensionState getState()
	{
		return (TooltipExtensionState) super.getState();
	}

	public void extend(AbstractComponent component)
	{
		if (component.getDescription() != null)
		{
			component.setDescription("");
		}
		super.extend(component);
	}

	public static void build(AbstractComponent component, String tooltipText)
	{
		TooltipExtension te = new TooltipExtension();
		te.getState().tooltipText = tooltipText == null ? "" : tooltipText;
		te.extend(component);
	}
	
	public static void buildDescriptionBased(AbstractComponent component)
	{
		build(component, component.getDescription());
	}
}
