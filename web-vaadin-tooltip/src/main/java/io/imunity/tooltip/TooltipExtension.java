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
	/**
	 * The way the console layout is styled currently confuses the tippy
	 * library causing to display the help in incorrect place - the number
	 * below is the size of the top bar plus default padding of form layout.
	 * There might be a need to calculate this number dynamically, but so far
	 * the fixed number is good enough.
	 */
	private static final int CONSOLE_TOP_OFFSET = -68;
	private static final String VAADIN_ICON_HTML = VaadinIcons.QUESTION.getHtml();

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

	public static void buildForConsole(AbstractComponent component, String tooltipText)
	{
		TooltipExtension te = new TooltipExtension();
		te.getState().tooltipText = tooltipText == null ? "" : tooltipText;
		te.getState().topOffset = CONSOLE_TOP_OFFSET;
		te.getState().vaadinIconHtml = VAADIN_ICON_HTML;
		te.extend(component);
	}

	public static void buildForConsoleDescriptionBased(AbstractComponent component)
	{
		buildForConsole(component, component.getDescription());
	}
}
