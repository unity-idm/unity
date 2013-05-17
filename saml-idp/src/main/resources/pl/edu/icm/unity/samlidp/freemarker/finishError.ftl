<#include "headerTop.ftl">


<H1>ERROR</H1>

<p>SAML Identity Provider got an invalid request.</p>
<p>If you are a user then you can be sure that the web application you was using previously is either misconfigured or buggy.</p>
<p>If you are an administrator or developer, here the details of the error follows:</p>
<p> ${error} </p>
<#if errorCause??>
	<p>Caused by: ${errorCause}</p>
</#if>

<#include "footer.ftl">