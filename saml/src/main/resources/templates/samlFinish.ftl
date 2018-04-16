<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/2002/REC-xhtml1-20020801/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
<title></title>
</head>
 
<#if samlError?? >
	<body onload="setTimeout('document.forms[0].submit()', 5000)">
<#else>
	<body onload="document.forms[0].submit()">
</#if>


<#if samlError?? >
	Problem occurred during authentication process.  
	<br/><br/>
	The error is: ${samlError?xhtml}
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


<form action="${samlService?xhtml}" method="post">
	<div>
	<#if RelayState??>
		<input type="hidden" name="RelayState" value="${RelayState?xhtml}"/>
	</#if>

	<input type="hidden" name="SAMLResponse" value="${SAMLResponse?xhtml}"/>
	</div>

	<noscript>
		<div>
		<input type="submit" value="Continue"/>
		</div>
	</noscript>

</form>

</body>
</html>