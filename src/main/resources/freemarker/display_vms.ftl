<!DOCTYPE html>

<html>
<head>
   <title>VM</title>
   <style>
       table, th, td {
       border: 1px solid black;
       }
   </style>
</head>
<body>
<form method="post" >

<table>
   <tr>
       <th>Name</th>
       <th>Instance ID</th>
       <th>Connection Status</th>
       <th>IP Address</th>
       <th>Alarm State</th>
       <th>Power State</th>
       <th>Launch Time</th>
       <th>Select VM</th>

   </tr>
   <#list VMs as vm>
       <tr>
           <#if (vm.name)??>
               <td>${vm["name"]}</td>
           <#else>
               <td>""</td>
           </#if>
           <#if (vm.instanceId)??>
               <td>${vm["instanceId"]}</td>
           <#else>
               <td>""</td>
           </#if>
           <#if (vm.conectionState)??>
               <td>${vm["conectionState"]}</td>
           <#else>
               <td>""</td>
           </#if>
           <#if (vm.ip)??>
               <td>${vm["ip"]}</td>
           <#else>
               <td>""</td>
           </#if>
           <#if (vm.alarmState)??>
               <td>${vm["alarmState"]}</td>
           <#else>
               <td>""</td>
           </#if>
           <#if (vm.powerState)??>
               <td>${vm["powerState"]}</td>
           <#else>
               <td>""</td>
           </#if>
           <#if (vm.launchTime)??>
               <td>${vm["launchTime"]}</td>
           <#else>
               <td>""</td>
           </#if>

            <td><input type="checkbox" name="${vm.name}" value="${vm.name}"></td>

       </tr>
   </#list>
</table>
  <button name = "PowerOn" type="submit">Power On</button>
  <button name = "PowerOff" type="submit">Power Off</button>
  <button name = "Delete" type="submit">Delete</button>
  <button name = "Create" type="submit">Create Instance</button>
  <button name = "Get_Chart" type="submit">More Info</button>

</form>
</body>
</html>
