package course;

import CONFIG.SJSULAB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.vmware.vim25.mo.*;
import org.bson.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;

/**
 * Created by vipul on 4/11/2015.
 */
public class statsCollector implements Runnable {


    MongoCredential cred = MongoCredential.createCredential("admin", "cmpe283project1", "admin".toCharArray());
    final MongoClient mongoClient = new MongoClient(new ServerAddress("ds061721.mongolab.com:61721"), Arrays.asList(cred));
    StatsDAO statsDAO= new StatsDAO(mongoClient.getDatabase("cmpe283project1"));


    public void run()
    {

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

        //while (true)
        {
            try {
                mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");

            if (mes == null || mes.length == 0) {
                return;
            }

            for (int i = 1; i < mes.length; i++) {

                VirtualMachine vm = (VirtualMachine) mes[i];
                String vmname=vm.getName();
                if(!(vmname.contains("Template")))
                {
                    statsDAO.addStats(vm);
                }
            }
            Thread.sleep(1000);
            } catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();

            }
        }
    }
}
