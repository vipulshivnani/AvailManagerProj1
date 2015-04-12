<!DOCTYPE html>

<html>
<head>
   <title>Create Instance</title>
   <style>
       table, th, td {
       border: 1px solid black;
       }
   </style>
</head>
<body>
<div id="create_vm">
Deploy Virtual Machine Instance
<div>

<form method="post">
   <table>
       <tr>
           <th></th>
           <th>Machine Type</th>
           <th>OS Version</th>
       </tr>
       <tr>
           <td><input type="radio" name="OS" value="T03-Win-Template"></td>
           <td>Windows</td>
           <td>Windows Server 2008 R2</td>
       </tr>
       <tr>
           <td><input type="radio" name="OS" value="T03-Lin-Template"></td>
           <td>Linux</td>
           <td>Ubuntu v10.04</td>
       </tr>
   </table>
   Name of the instance:<input type="text" name="vmname"><br>
   <button name = "Create" type="submit">Create</button>
   <button name = "Cancle" type="submit">Cancle</button>
</form>
</body>
</html>
