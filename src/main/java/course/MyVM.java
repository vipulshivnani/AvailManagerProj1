package course;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import com.vmware.vim25.ComputeResourceConfigSpec;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.VirtualMachineCapability;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;


import CONFIG.*;

/**
 * Write a description of class MyVM here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class MyVM
{
	// instance variables 
	private  String vmname ;
	private ServiceInstance si ;
	private VirtualMachine vm ;
	private Folder rootFolder;
	private String snapshotname;
	private String clonename;
	boolean result =false;

	/**
	 * Constructor for objects of class MyVM
	 */
	public MyVM( String vmname ) 
	{
		// initialise instance variables
		try {
			this.vmname = vmname;
			this.si = new ServiceInstance(new URL(SJSULAB.getVmwareHostURL()),
					SJSULAB.getVmwareLogin(), SJSULAB.getVmwarePassword(), true);
			rootFolder = si.getRootFolder();
		
			this.vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);
			//HostSystem newHost = (HostSystem) new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem",newHostName);

			// your code here
		} catch ( Exception e ) 
		{ System.out.println( e.toString() ) ; }

	}


	/**
	 * Power On the Virtual Machine
	 */
	public void powerOn() 
	{
		try {
			System.out.println("Powering on virtual machine '"+vm.getName() +"'. Please wait...");     
			Task t=vm.powerOnVM_Task(null);
			if(t.waitForTask()== Task.SUCCESS)
			{
				System.out.println("Virtual machine powered on.");
			} 
		}catch ( Exception e ) 
		{ System.out.println( e.toString() ) ; }
	}




	public void vmHealth(){
		try{

			long start = System.currentTimeMillis();
			URL url = new URL("https://130.65.132.103/sdk");
			ServiceInstance si = new ServiceInstance(url, "administrator", "12!@qwQW", true);
			long end = System.currentTimeMillis();
			System.out.println("time taken:" + (end-start));
			Folder rootFolder = si.getRootFolder();
			String name = rootFolder.getName();
			System.out.println("root:" + name);
			//ManagedEntity mes = new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);



			//VirtualMachine vm = (VirtualMachine) mes; 

			VirtualMachineConfigInfo vminfo = vm.getConfig();
			VirtualMachineCapability vmc = vm.getCapability();

			vm.getResourcePool();
			System.out.println("==========="+vm.getName()+"===========");

			System.out.println("GuestOS: " + vminfo.getGuestFullName());
			System.out.println("Multiple snapshot supported: " + vmc.isMultipleSnapshotsSupported());
			System.out.println("VM Guest IP: " +vm.getGuest().getIpAddress());
			

			
		}
		catch(Exception e){

		}


	}

	public void createAlarm()
	{
		try {
			//vmname=this.vmname;

			CreateVmAlarm.createAlarm(si,vm,vmname);

		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	public boolean pingAll(String ip)
	{
		boolean res= false;
		String cmd = "ping "+ ip;
		String consoleResult="";
		try
		{
			if(ip!=null)
			{
				Runtime r=Runtime.getRuntime();
				Process p= r.exec(cmd);

				BufferedReader input= new BufferedReader(new InputStreamReader(p.getInputStream()));
				while(input.readLine()!=null)
				{
					System.out.println(input.readLine());
					consoleResult+=input.readLine();	    				
				}
				input.close();

				if(consoleResult.contains("Request timed out"))
				{
					System.out.println("Packets Dropped");
					res=false;
					//flag=false;
				}
				else
				{
					//ping successful
					System.out.println("ping successful ");
					res=true;
					//notifyAll();
				}

			} 
			else 
			{
				System.out.println("IP is not found!");
				res = false; //ip = null
				//flag=false;
			}
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
		return res;
	}
	public boolean pingVM(){
		result=false;
		String ip;

		try
		{

			//ManagedEntity mes = new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);
			//VirtualMachine vm= (VirtualMachine) mes;

			ip=vm.getGuest().getIpAddress();

			result=pingAll(ip);
			return result;
		}	
		catch(Exception e){
			return result;
		}
		//return result;
	}

	public boolean checkAlarmStatus(){
		boolean alramStatus= false;
		if(vm.getTriggeredAlarmState()!=null)
		{
			alramStatus= true;
		}

		return alramStatus;
	}
	//recheck
	public boolean pingHost() {
		result=false;
		try
		{
			System.out.println("Calling ping Host");
			
			ManagedEntity[] hosts = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
	
			VirtualMachineRuntimeInfo vmri = vm.getRuntime();
			String hostB=vmri.getHost().get_value();
			
			for(int i=0;i<hosts.length;i++)
			{
				//System.out.println("value=" + hosts[i].getMOR().get_value());
				
				String hostA=hosts[i].getMOR().get_value();
				if(hostA.equalsIgnoreCase(hostB))
				{ 
					String ip= hosts[i].getName();
					System.out.println("The ip of parent host is  " + ip);
					System.out.println("Trying to ping vHost...");
					result= pingAll(ip);
					return result;
				}
				else
				{
					System.out.println("Not able to find the corresponding host...");
					result=false;
				}
			}

		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}

		return result;

	}
	public void checkVmState(){
		try{
			//ManagedEntity mes = new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);
			//VirtualMachine vm= (VirtualMachine) mes;

			VirtualMachineRuntimeInfo vmri=vm.getRuntime();
			String state=vmri.getPowerState().toString();
			if(state.contains("poweredOn")){
				System.out.println("The Virtual machine is powered on");
			}

			else {
				powerOn();
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void createOneSnapshot() throws Exception{ 
		try { 
			this.snapshotname=vmname + "_SS";
			System.out.println("Please wait.. your snapshot is being created...."); 
			VirtualMachineSnapshot vmSnap= vm.getCurrentSnapShot();
			if(vmSnap !=null) {
				Task task = vmSnap.removeSnapshot_Task(true);
			
				Task t = vm.createSnapshot_Task(snapshotname, "my vm 's snapshot", true, false); 
				if(t.waitForTask()==t.SUCCESS) { 
					System.out.println("Snapshot is created successfully"); 
				} else { 
					System.out.println("not yet created........................");
				} 


			}
			else{
				Task t = vm.createSnapshot_Task(snapshotname, "my vm 's snapshot", true, false); 
				if(t.waitForTask()==t.SUCCESS) { 
					System.out.println("snapshot created"); 
				} else { 
					System.out.println("not yet created........................");
				} 
			}

		} 
		catch(Exception e) {
			System.out.println(e.toString()); } }

	// check the state of machine -- is it powering on??
	public void revertToSnapshot() throws Exception {

		try 
		{
			Task task= vm.getCurrentSnapShot().revertToSnapshot_Task(null);
			

			if (task.waitForTask() == Task.SUCCESS) 
			{
				System.out.println("Reverted to snapshot");	
				checkVmState();
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}
	/*else{
			createOneSnapshot();
			revertToSnapshot(); // calling itself
		}*/

	public String addNewHost(String ip)
	{
		String ret = "";
		try 
		{	

			ManagedEntity [] mes =  new InventoryNavigator(rootFolder).searchManagedEntities("Datacenter");
			Datacenter dc = new Datacenter(rootFolder.getServerConnection(),  mes[0].getMOR());
			HostConnectSpec hs = new HostConnectSpec();
			//String ip= "130.65.133.72";
			hs.hostName= ip;
			hs.userName ="root";
			hs.password = "12!@qwQW";
			hs.managementIp = "130.65.132.103";
			hs.setSslThumbprint("C5:EF:CA:98:96:80:6D:2E:46:CB:B1:D2:BB:87:4A:18:AF:26:83:20");
			//hs.setSslThumbprint("90:BD:8C:C1:4E:F6:E9:A3:1A:DF:4B:FA:16:6B:9A:0D:73:DC:6A:F7");
			ComputeResourceConfigSpec crcs = new ComputeResourceConfigSpec();
			Task t = dc.getHostFolder().addStandaloneHost_Task(hs,crcs, true);
			if(t.waitForTask() == t.SUCCESS)
			{
				ret = ip;
			}
			else
			{
				ret = "";
			}


		}   
		catch (Exception re)
		{
			System.out.println(re.toString());
			System.out.println("Unable to connect to Vsphere server");
		}
		return ret;
	}
	public String searchHost()
	{
		//boolean present=false;
		String ip="";
		try
		{
			ManagedEntity[] hosts = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
			if(hosts.length <=1) 
			{
				System.out.println("There is only one host present");
				//return false;
			}
			else
			{
				System.out.println("Multiple hosts present.. Searching in vCenter..");
			}


			VirtualMachineRuntimeInfo vmri = vm.getRuntime();
			String hostB=vmri.getHost().get_value();
			System.out.println("Lists of new hosts: ");
			for(int i=0;i<hosts.length;i++)
			{
				String hostA=hosts[i].getMOR().get_value();

				if(!(hostA.equalsIgnoreCase(hostB)))
				{ 
					String hostIp= hosts[i].getName();
					System.out.println("Host "+(i+1) + " : "+hostIp);
					System.out.println("Trying to ping new vHost...");
					boolean res = pingAll(hostIp);
					if(res)
					{
						System.out.println("Your new host is live! Migrating now...");
						ip=hostIp;
						return ip;
					}
					else
					{
						System.out.println("New host not live.. Should try to ping another host...");
					}
				}

			}

		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}

		return ip;


	}
	public void migrateToNewHost( String ip)
	{
		String newHostIp= ip;
		HostSystem newHost;

		try
		{
			VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);

			newHost = (HostSystem) new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem",newHostIp);
			ComputeResource cr = (ComputeResource) newHost.getParent();

			Task task = vm.migrateVM_Task(cr.getResourcePool(),newHost,	VirtualMachineMovePriority.highPriority,
					VirtualMachinePowerState.poweredOff);

			if(task.waitForTask() == task.SUCCESS)
			{
				System.out.println("Migration to new host completed.");
				//					
			}

		} 
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	


}

