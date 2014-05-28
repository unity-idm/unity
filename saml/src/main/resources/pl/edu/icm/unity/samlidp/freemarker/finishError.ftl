<#include "headerTop.ftl">

<body>
<H1>ERROR</H1>

<p>SAML Identity Provider got an invalid request.</p>
<p>If you are a user then you can be sure that the web application you was using previously is either misconfigured or buggy.</p>
<p>If you are an administrator or developer, here the details of the error follows:</p>
<p> ${error?xhtml} </p>
<#if errorCause??>
	<p>Caused by: ${errorCause?xhtml}</p>
</#if>

<#include "footer.ftl">