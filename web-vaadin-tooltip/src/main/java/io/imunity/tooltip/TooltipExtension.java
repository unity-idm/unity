package io.imunity.tooltip;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.AbstractComponent;

import io.imunity.tooltip.client.TooltipExtensionState;

@JavaScript({ "vaadin://popper-2.4.0.min.js", "vaadin://tippy-6.2.2.umd.min.js" })
@StyleSheet({ "vaadin://tippy.css" })
public class TooltipExtension extends AbstractJavaScriptExtension
{
	private static final String VAADIN_ICON_HTML = VaadinIcons.QUESTION_CIRCLE_O.getHtml();

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

	public static void tooltip(AbstractComponent component, String tooltipText)
	{
		TooltipExtension te = new TooltipExtension();
		te.getState().tooltipText = tooltipText == null ? "" : tooltipText;
		te.getState().vaadinIconHtml = VAADIN_ICON_HTML;
		if (component.getWidth() != 0 && component.getWidthUnits() != null)
		{
			te.getState().baseElementWidth = component.getWidth() + component.getWidthUnits().getSymbol();
		}
		te.extend(component);
	}

	public static void tooltip(AbstractComponent component)
	{
		tooltip(component, component.getDescription());
	}
}
