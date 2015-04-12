<!DOCTYPE html>
<html>
<head>
    <title>VM statistics</title>
</head>
<body>

<h1>VM statistics</h1>

<#list vms as vm>
    <#if (vm.MaxCPUusage)??>
<h6>MaxCpuUsage :${vm["MaxCPUusage"]} </h6>
    </#if>
    <#if (vm.MaxMemoryUsage)??>
<h6>MaxMemoryUsage :${vm["MaxMemoryUsage"]} </h6>

    </#if>
--------------


</#list>
</body>
</html>

