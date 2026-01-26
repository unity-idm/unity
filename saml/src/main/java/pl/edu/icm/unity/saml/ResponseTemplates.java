package pl.edu.icm.unity.saml;

public enum ResponseTemplates
{
	POST_BINDING_TMPL("postBinding.ftl"),
	FINISH_TMPL("samlFinish.ftl");

	public final String templateFile;

	ResponseTemplates(String templateFile)
	{
		this.templateFile = templateFile;
	}
}
