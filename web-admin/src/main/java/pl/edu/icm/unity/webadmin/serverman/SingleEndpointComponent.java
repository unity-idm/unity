package pl.edu.icm.unity.webadmin.serverman;

import java.util.Map;

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

		header.addComponent(showhide);
		header.setComponentAlignment(showhide, Alignment.BOTTOM_LEFT);

		// Name
		HorizontalLayout nameFieldLayout = addFieldWithLabel(header,
				msg.getMessage("EndpointsStatus.name"), endpoint.getId(), 0);
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

		HorizontalLayout lt = addFieldWithLabel(content,
				msg.getMessage("EndpointsStatus.type"), endpoint.getType()
						.getName(), 19);
		addFieldWithLabel(lt, msg.getMessage("EndpointsStatus.type") + " "
				+ msg.getMessage("EndpointsStatus.description").toLowerCase(),
				endpoint.getType().getDescription(), 2);

		if (endpoint.getDescription() != null && endpoint.getDescription().length() > 0)
		{
			addFieldWithLabel(content, msg.getMessage("EndpointsStatus.description"),
					endpoint.getDescription(), 19);

		}
		addFieldWithLabel(content, msg.getMessage("EndpointsStatus.contextAddress"),
				endpoint.getContextAddress(), 19);

		int i = 0;

		addFieldWithLabel(content, msg.getMessage("EndpointsStatus.paths"), "", 19);

		for (Map.Entry<String, String> entry : endpoint.getType().getPaths().entrySet())
		{
			i++;
			HorizontalLayout l = addFieldWithLabel(content, String.valueOf(i),
					endpoint.getContextAddress() + entry.getKey(), 55);
			addFieldWithLabel(l, msg.getMessage("EndpointsStatus.description"),
					entry.getValue(), 2);

		}

		StringBuilder bindings = new StringBuilder();
		for (String s : endpoint.getType().getSupportedBindings())
		{
			if (bindings.length() > 0)

				bindings.append(",");
			bindings.append(s);

		}
		// Bindings
		addFieldWithLabel(content, msg.getMessage("EndpointsStatus.bindings"),
				bindings.toString(), 19);

		i = 0;
		addFieldWithLabel(content, msg.getMessage("EndpointsStatus.authenticatorsSet"), "",
				19);
		for (AuthenticatorSet s : endpoint.getAuthenticatorSets())
		{
			i++;
			StringBuilder auth = new StringBuilder();
			for (String a : s.getAuthenticators())
			{
				if (auth.length() > 0)
					auth.append(",");
				auth.append(a);
			}
			addFieldWithLabel(content, String.valueOf(i), auth.toString(), 55);
			// Authenticators

		}

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
			int space)
	{
		if (space != 0)
		{
			Label spacer = new Label();
			spacer.setWidth(space, Unit.PIXELS);
		}
		Label namel = new Label(name + ":");
		namel.addStyleName(Styles.bold.toString());
		Label nameVal = new Label(value);
		HorizontalLayout fieldLayout = new HorizontalLayout();
		fieldLayout.setSpacing(true);

		if (space != 0)
		{
			Label spacer = new Label();
			spacer.setWidth(space, Unit.PIXELS);
			fieldLayout.addComponents(spacer, namel, nameVal);
		} else
		{
			fieldLayout.addComponents(namel, nameVal);
		}

		parent.addComponent(fieldLayout);
		return fieldLayout;

	}

}
