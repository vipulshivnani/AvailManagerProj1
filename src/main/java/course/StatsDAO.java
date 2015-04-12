/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package course;

import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.VirtualMachine;
import org.bson.BSON;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.json.JSONException;
import org.json.JSONObject;
import sun.misc.BASE64Encoder;

import javax.print.Doc;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

public class StatsDAO {
    private final MongoCollection<Document>statsCollection;
    private Random random = new SecureRandom();

    public StatsDAO(final MongoDatabase blogDatabase) {
        statsCollection = blogDatabase.getCollection("Stats");
    }

    // validates that username is unique and insert into db
    public boolean addStats(VirtualMachine vm) {
        Date time = new Date();
        Document stats = new Document("_id", time);

        VirtualMachineRuntimeInfo vmri = vm.getRuntime();
        VirtualMachineConfigInfo vmci= vm.getConfig();
        SimpleDateFormat sdf = new SimpleDateFormat("mm");
        if(vmri.getMaxCpuUsage()!=null && vmri.getMaxMemoryUsage()!=null ) {
            stats.append("Name", vm.getName());
            stats.append("MaxCPUusage", vmri.getMaxCpuUsage().toString());
            stats.append("MaxMemoryUsage", vmri.getMaxMemoryUsage().toString());
            stats.append("IpAdd",vm.getGuest().getIpAddress());
            stats.append("Date",sdf.format(new Date()));
        }
        //stats.append("Host", vmri.getHost().toString());

        System.out.println("MaxCPUusage :" + vmri.getMaxCpuUsage());
        System.out.println("MaxMemoryUsage:" + vmri.getMaxMemoryUsage());


        statsCollection.insertOne(stats);
    return false;
    }

    // validates that username is unique and insert into db
    public List<Document> getStats() {

        //List<Document> vms=new ArrayList<Document>();


        List<Document> vms=statsCollection.find().sort(new BasicDBObject()).limit(50).into(new ArrayList<Document>());

        return vms;
    }

   public ArrayList<ArrayList> getGchart(String vm){

       List<Document> gcdata;
        BasicDBObject query = new BasicDBObject();
       query.put("Name", vm);
       //gcdata=statsCollection.find(query).into(new ArrayList<Document>());

       //System.out.println(gcdata);


       MongoCursor<Document> cursor= statsCollection.find(query).iterator();

        Integer i=0;
       ArrayList<ArrayList> finalGraph=new ArrayList<ArrayList>();
       while (cursor.hasNext()){

           String a=cursor.next().toJson();
           System.out.println(a);

           try {
               JSONObject jsonObject=  new JSONObject(a);
               ArrayList<String> drawRow= new ArrayList<String>();
               drawRow.add(i.toString());
               i++;
               if (jsonObject.has("MaxMemoryUsage")&& jsonObject.has("MaxCPUusage")) {
                   drawRow.add(jsonObject.getString("MaxMemoryUsage"));
                   drawRow.add(jsonObject.getString("MaxCPUusage"));
               }
               finalGraph.add(drawRow);
           } catch (JSONException e) {
               e.printStackTrace();
           }


           // memoryData.add(Integer.parseInt(a.g))
       }

       return finalGraph;
   }



    public List<Integer> getMemoryData() {

        //List<Document> vms=new ArrayList<Document>();


        List<Integer> memoryData=new ArrayList<Integer>();
        BasicDBObject query= new BasicDBObject();
        query.put("IpAdd", "130.65.133.244");

        Document filter= new Document();
        filter.append("MaxMemoryUsage", 1);
        MongoCursor<Document> cursor= statsCollection.find(query).iterator();


        System.out.println("Inside test");

        while (cursor.hasNext()){

        String a=cursor.next().toJson();
            System.out.println(a);

            try {
                JSONObject jsonObject=  new JSONObject(a);
                memoryData.add(Integer.parseInt(jsonObject.getString("MaxMemoryUsage")));
            } catch (JSONException e) {
                e.printStackTrace();
            }


            // memoryData.add(Integer.parseInt(a.g))
        }
       return memoryData;
    }

    public List<Document> findByDateDescending(int limit) {
        // XXX HW 3.2, Work Here
        // Return a list of DBObjects, each one a post from the stats collection
        //List<Document> posts = (DBObject) statsCollection.find().sort(new BasicDBObject("date",-1)).limit(limit).into(new ArrayList<Document>());

        return null;
    }
}
