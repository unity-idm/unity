<#include "headerTop.ftl">


<#if samlError?? >
	<body onload="setTimeout('document.forms[0].submit()', 5000)">
<#else>
	<body onload="document.forms[0].submit()">
</#if>


<#if samlError?? >
	Problem occurred during authentication process.  
	<br/><br/>
	The error is: ${samlError}
	<p>
	You will be automatically redirected in 5s back to the service which requested 
	authentication, with the above information.
	</p>
</#if>


<noscript>
	<p>
	<strong>Note:</strong> Since your browser does not support JavaScript,
	you must press the Continue button once to proceed.
	</p>
</noscript>


<form action="${samlService}" method="post">
	<div>
	<#if RelayState??>
		<input type="hidden" name="RelayState" value="${RelayState?url}"/>
	</#if>

	<input type="hidden" name="SAMLResponse" value="${SAMLResponse}"/>
	</div>

	<noscript>
		<div>
		<input type="submit" value="Continue"/>
		</div>
	</noscript>

</form>

<#include "footer.ftl">