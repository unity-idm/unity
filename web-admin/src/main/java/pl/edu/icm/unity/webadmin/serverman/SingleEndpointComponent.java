package pl.edu.icm.unity.webadmin.serverman;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

public class SingleEndpointComponent extends CustomComponent
{
	private EndpointDescription endpoint;
	private UnityMessageSource msg;

	public SingleEndpointComponent(EndpointDescription endpoint, UnityMessageSource msg)
	{
		this.endpoint = endpoint;
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();

		GridLayout header = new GridLayout(5, 1);
		header.setSpacing(true);

		final Button showhide = new Button();
		showhide.setIcon(Images.zoomin.getResource());
		showhide.addStyleName(Reindeer.BUTTON_LINK);
		showhide.addStyleName(Styles.toolbarButton.toString());

		header.addComponent(showhide);
		header.setComponentAlignment(showhide, Alignment.BOTTOM_LEFT);

		// Name
		HorizontalLayout nameFieldLayout = addFieldWithLabel(header, "name",
				endpoint.getId(), false);
		nameFieldLayout.setMargin(false);
		header.setComponentAlignment(nameFieldLayout, Alignment.BOTTOM_CENTER);

		// Status
		Image status = new Image("", Images.ok.getResource());
		Label statusLabel = new Label(msg.getMessage("EndpointsStatus.status") + ":");
		statusLabel.addStyleName(Styles.bold.toString());
		header.addComponent(statusLabel);
		header.setComponentAlignment(statusLabel, Alignment.BOTTOM_CENTER);
		header.addComponent(status);

		main.addComponent(header);

		final VerticalLayout content = new VerticalLayout();

		StringBuilder bindings = new StringBuilder();
		for (String s : endpoint.getType().getSupportedBindings())
		{
			if (bindings.length() > 0)

				bindings.append(",");
			bindings.append(s);

		}

		if (endpoint.getDescription() != null && endpoint.getDescription().length() > 0)
		{
			addFieldWithLabel(content, "description", endpoint.getDescription(), true);

		}
		addFieldWithLabel(content, "contextAddress", endpoint.getContextAddress(), true);

		// Bindings
		addFieldWithLabel(content, "bindings", bindings.toString(), true);

		StringBuilder auth = new StringBuilder();
		for (AuthenticatorSet s : endpoint.getAuthenticatorSets())
		{
			for (String a : s.getAuthenticators())
			{
				if (auth.length() > 0)
					auth.append(",");
				auth.append(a);
			}
		}

		// Authenticators
		addFieldWithLabel(content, "authenticators", auth.toString(), true);

		content.setVisible(false);
		main.addComponent(content);
		setCompositionRoot(main);

		showhide.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				if (content.isVisible())
				{
					showhide.setIcon(Images.zoomin.getResource());
					content.setVisible(false);
				} else
				{
					showhide.setIcon(Images.zoomout.getResource());
					content.setVisible(true);
				}

			}
		});

	}

	private HorizontalLayout addFieldWithLabel(Layout parent, String name, String value,
			boolean useSpace)
	{
		if (useSpace)
		{
			Label spacer = new Label();
			spacer.setWidth(19, Unit.PIXELS);
		}
		Label namel = new Label(msg.getMessage("EndpointsStatus." + name) + ":");
		namel.addStyleName(Styles.bold.toString());
		Label nameVal = new Label(value);
		HorizontalLayout fieldLayout = new HorizontalLayout();
		fieldLayout.setSpacing(true);

		if (useSpace)
		{
			Label spacer = new Label();
			spacer.setWidth(19, Unit.PIXELS);
			fieldLayout.addComponents(spacer, namel, nameVal);
		} else
		{
			fieldLayout.addComponents(namel, nameVal);
		}

		parent.addComponent(fieldLayout);
		return fieldLayout;

	}

}
