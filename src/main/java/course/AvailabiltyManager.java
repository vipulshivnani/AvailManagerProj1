package course;

import java.net.MalformedURLException;
import java.net.URL;

import CONFIG.SJSULAB;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.vmware.vim25.HostConfigInfo;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

import CONFIG.*;

public class AvailabiltyManager implements Runnable   {

	/**
	 * @param args
	 *///public static void main (String args[]) throws Exception{
	private Thread t1;

	final MyVM myvm = new MyVM("T03-VM01-Lin");
	final MyHost myhs= new MyHost("T03-vHost01_132.151");

	MongoCredential cred = MongoCredential.createCredential("admin","cmpe283project1", "admin".toCharArray());
	final MongoClient mongoClient = new MongoClient(new ServerAddress("ds061721.mongolab.com:61721"), Arrays.asList(cred));
	final MongoDatabase blogDatabase = mongoClient.getDatabase("cmpe283project1");

	//StatsDAO userDAO = new StatsDAO(mongoClient.getDB("cmpe283project1"));

	
	public void run(){
    
	    //RealtimePerfMonitor.StatsCollection();
        long start = System.currentTimeMillis();
        ServiceInstance si = null;
		try {
			si = new ServiceInstance(new URL(SJSULAB.getVmwareHostURL()), SJSULAB.getVmwareLogin(), SJSULAB.getVmwarePassword(), true);
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        long end = System.currentTimeMillis();
        System.out.println("time taken:" + (end-start));
        Folder rootFolder = si.getRootFolder();
        String name = rootFolder.getName();
        System.out.println("root:" + name);
        ManagedEntity[] mes = null;
		try {
			mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        if(mes == null || mes.length ==0)
        {
            return;
        }

        for (int i = 1; i < mes.length; i++){

        	VirtualMachine vm = (VirtualMachine) mes[i];

			//userDAO.addStats(vm);
			//userDAO.getStats();
	        VirtualMachineConfigInfo vminfo = vm.getConfig();
	        VirtualMachineCapability vmc = vm.getCapability();

			VirtualMachineRuntimeInfo vmri = vm.getRuntime();

			try {
				vm.getResourcePool();
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        System.out.println("Hello " + vm.getName());
	        
	        String VM_name = vminfo.getGuestFullName();
	        if (!(VM_name.contains("Template"))){
	        	System.out.println("GuestOS: " + vminfo.getGuestFullName());
	        	System.out.println("Multiple snapshot supported: " + vmc.isMultipleSnapshotsSupported());
		        String ip = vm.getGuest().getIpAddress();
		        System.out.println("IP address : " + ip);
		        if (ip == null)
					try {
						vm.powerOnVM_Task(null);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        }
	        if (VM_name.equals("T03-VM01-Lin"))
	        	break;

        }
        
        si.getServerConnection().logout();

        try {
        	Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
		try{
			//myvm.createOneSnapshot();
			//myhs.createHostSnapshot();
			//myvm.createAlarm();
			int ping_count = 0; 
			for(int i = 0; i < 3; i++){
				if(myvm.pingVM())
					ping_count++;
			}
			if(ping_count == 3){
				System.out.println("VM is Up and Running Details of VM.... ");
				myvm.vmHealth();
			}
			else{
				ping_count = 0;
				for(int i = 0; i < 3; i++){
					if(myvm.pingHost())
						ping_count++;
				}
				if(ping_count == 3){
 					if(myvm.checkAlarmStatus()){
						System.out.println("Legitimate power off of the VM. Wait until it is powered on again by the user.");
					}				
					else{
						System.out.println("The parent host is up and running, getting VM from current Snapshot ");
						myvm.revertToSnapshot();
						Thread.sleep(60000);
					    System.out.println("New virtual Machine is available now");
					    myvm.vmHealth();
					}
				}
				else{
					System.out.println("The parent host is not alive, trying to getting it live. Wait......");	
					myhs.revertHostToSnapshot();
					System.out.println("It will take time to reflect changes will reflect. Wait....");
					Thread.sleep(180000);
					boolean checkHost=myvm.pingHost();
					if(!checkHost){
						System.out.println("Not able to connect the current host, Try with other host of Data Center");	
						String ip=myvm.searchHost();
						if(ip.isEmpty())
						{
							System.out.println("This was only one host so trying to add another host to datacenter");
							ip = myvm.addNewHost("130.65.132.152");
							Thread.sleep(50000);
						}
						System.out.println("Migrating the vm to another available host with ip : "+ ip);
						myvm.migrateToNewHost( ip);
						Thread.sleep(180000);
					}
					if(myvm.pingVM()){
						System.out.println("Vm is up and Running and availble now");
						myvm.vmHealth();
					}

				}
			}
			
		}
		catch(Exception e){
			e.printStackTrace();
		}

		/*try{
				Thread.sleep(7000);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		 */		
	}
	public void start(){
		t1 = new Thread (this);
		t1.run();
	}

}




