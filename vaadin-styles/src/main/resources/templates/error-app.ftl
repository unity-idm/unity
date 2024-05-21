<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title> Error </title>
  </head>
 
  <body>

    <H1> <#if subsystem??>${subsystem?html}</#if> ERROR</H1>

    <#if generalInfo??>
      <p>${generalInfo?html}</p>
    </#if>

    <p> ${error?html} </p>


    <#if errorCause??>
      </small>
      <p>Administrator/developer oriented details of the error follows:</p>
	  <p><pre>Caused by: ${errorCause?html}</pre></p>
	  </small>
    </#if>
    
  </body>
</html>