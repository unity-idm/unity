package pl.edu.icm.unity.stdext.mconsumers;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.notifications.MessageTemplateConsumer;

public class PassowordResetMsgConsumer implements MessageTemplateConsumer
{

	@Override
	public String getDescription()
	{
		return "Password reset message";
	}

	@Override
	public String getName()
	{
		return "passwordResetCode";
	}

	@Override
	public Map<String, String> getVariables()
	{
		Map <String, String> vars = new HashMap<String, String>();
		vars.put("user", "Username");
		vars.put("code", "Code to password reset");
		return vars;
	}

}
