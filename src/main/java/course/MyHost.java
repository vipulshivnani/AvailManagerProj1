package course;

import java.net.URL;

import CONFIG.SJSULAB;

import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;


public class MyHost {
	private String vmname ;
	private ServiceInstance si ;
	private VirtualMachine vm ;
	private Folder rootFolder;
	private String snapshotname;
	private String clonename;
	boolean result;

	
	public MyHost( String vmname ) 
	{
		// initialise instance variables
		try {
			this.vmname = vmname;
			this.si = new ServiceInstance(new URL(SJSULAB.getVmwareHostURL1()),
					SJSULAB.getVmwareLogin1(), SJSULAB.getVmwarePassword1(), true);
		rootFolder = si.getRootFolder();
			this.vm = (VirtualMachine) new InventoryNavigator(rootFolder)
			.searchManagedEntity("VirtualMachine", SJSULAB.getVmwareVM1());
			

			// your code here
		} catch ( Exception e ) 
		{ System.out.println( e.toString() ) ; }

	}

	public void createHostSnapshot() throws Exception{ 
		try { 
			this.snapshotname=vmname + "_HostSnapShot";
			System.out.println("Please wait.. your snapshot is being created...."); 
			VirtualMachineSnapshot vmSnap= vm.getCurrentSnapShot();
			if(vmSnap !=null) {
				Task task = vmSnap.removeSnapshot_Task(true);
				
				Task t = vm.createSnapshot_Task(snapshotname, "my vm 's snapshot", true, false); 
				if(t.waitForTask()==t.SUCCESS) { 
					System.out.println("Snapshot of host is created successfully"); 
				} else { 
					System.out.println("not yet created........................");
				} 


			}
			else{
				Task t = vm.createSnapshot_Task(snapshotname, "my vm 's snapshot", true, false); 
				if(t.waitForTask()==t.SUCCESS) { 
					System.out.println("Snapshot of host is created successfully"); 
				} else { 
					System.out.println("not yet created........................");
				} 
			}

		} 
		catch(Exception e) {
			System.out.println(e.toString()); } }

	// check the state of machine -- is it powering on??
	public void revertHostToSnapshot() throws Exception {
		try 
		{
			Task task= vm.getCurrentSnapShot().revertToSnapshot_Task(null);
			System.out.println("Before Calling");
			
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
	public void checkVmState(){
		try{
			ManagedEntity mes = new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);
			VirtualMachine vm= (VirtualMachine) mes;

			VirtualMachineRuntimeInfo vmri=vm.getRuntime();
			String state=vmri.getPowerState().toString();
			if(state.contains("poweredOn")){
				System.out.println("The host is powered on");
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
}
