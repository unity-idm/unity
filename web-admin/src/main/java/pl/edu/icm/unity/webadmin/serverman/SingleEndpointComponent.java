package pl.edu.icm.unity.webadmin.serverman;

import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.shared.ui.AlignmentInfo.Bits;
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
import com.vaadin.ui.themes.BaseTheme;

public class SingleEndpointComponent extends CustomComponent
{
	private EndpointDescription endpoint;

	public SingleEndpointComponent(EndpointDescription endpoint)
	{
		this.endpoint = endpoint;
		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();

		GridLayout header = new GridLayout(5, 1);
		header.setSpacing(true);

		final Button showhide = new Button();
		showhide.setIcon(Images.zoomout.getResource());
		showhide.addStyleName(BaseTheme.BUTTON_LINK);

		header.addComponent(showhide);
		header.setComponentAlignment(showhide, new Alignment(Bits.ALIGNMENT_RIGHT
				| Bits.ALIGNMENT_BOTTOM));

		//Name
		Label namel = new Label("Name" + ":");
		namel.addStyleName(Styles.bold.toString());
		Label nameVal = new Label(endpoint.getId());
		HorizontalLayout nf = new HorizontalLayout();
		nf.setSpacing(true);
		nf.addComponents(namel, nameVal);
		header.addComponent(nf);
		header.setComponentAlignment(nf, Alignment.BOTTOM_CENTER);
		
		//Status
		Image status = new Image("", Images.ok.getResource());
		namel = new Label("Status" + ":");
		namel.addStyleName(Styles.bold.toString());
		
		header.addComponent(namel);
		header.setComponentAlignment(namel, Alignment.BOTTOM_CENTER);
		header.addComponent(status);
	        
		main.addComponent(header);

		final VerticalLayout content = new VerticalLayout();

		StringBuilder bindings = new StringBuilder();

		for (String s : endpoint.getType().getSupportedBindings())
		{
			if (bindings.length() > 0)

				bindings.append(" | ");
			bindings.append(s);

		}

		if (endpoint.getDescription() != null && endpoint.getDescription().length() > 0)
		{
			addNamedField(content, "Description", endpoint.getDescription());

		}
		addNamedField(content, "Address", endpoint.getContextAddress());
		addNamedField(content, "Bindings", bindings.toString());

		StringBuilder auth = new StringBuilder();

		for (AuthenticatorSet s : endpoint.getAuthenticatorSets())
		{
			for (String a : s.getAuthenticators())
			{
				if (auth.length() > 0)
					auth.append(" | ");
				auth.append(a);
			}
		}

		addNamedField(content, "Authenticators", auth.toString());

		content.setVisible(true);
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

	private HorizontalLayout addNamedField(Layout parent, String name, String value)
	{
		
		Label space = new Label();
		space.setWidth(18,Unit.PIXELS);
		Label namel = new Label(name + ":");
		namel.addStyleName(Styles.bold.toString());
		Label nameVal = new Label(value);
		HorizontalLayout nf = new HorizontalLayout();
		nf.setSpacing(true);
		nf.addComponents(space,namel, nameVal);
		parent.addComponent(nf);
		return nf;

	}

}
