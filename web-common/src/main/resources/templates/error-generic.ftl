<!DOCTYPE html>
<html>
  <head>
    <#include "system/header-error.ftl">
  </head>
 
  <body>

    <h1 style="color: red;">Error: ${errorCode?html}</h1>
    
    <p>Error reason:</p>
    <pre>${error?html}</pre>
    
  </body>
</html>