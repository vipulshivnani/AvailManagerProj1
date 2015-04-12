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

import CONFIG.SJSULAB;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.*;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.http.Cookie;
import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.setPort;

/**
 * This class encapsulates the controllers for the blog web application.  It delegates all interaction with MongoDB
 * to three Data Access Objects (DAOs).
 * <p/>
 * It is also the entry point into the web application.
 */
public class BlogController {
    private final UserDAO userDAO;
    private final SessionDAO sessionDAO;
    private final Configuration cfg;
    private String url = "https://130.65.132.103/sdk";
    private ServiceInstance si;
    private Folder rootFolder;
    private StatsDAO statsDAO;
    private String CurrentSelectedVM;

    private final MongoCollection<Document> VMsDBCollection;

    private Random random = new SecureRandom();



    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            new BlogController("mongodb://admin:admin@ds061721.mongolab.com:61721");

        }
        else {
            new BlogController(args[0]);
        }
    }

    public BlogController(String mongoURIString) throws IOException {
        MongoCredential cred = MongoCredential.createCredential("admin","cmpe283project1", "admin".toCharArray());
        final MongoClient mongoClient = new MongoClient(new ServerAddress("ds061721.mongolab.com:61721"), Arrays.asList(cred));
        final MongoDatabase blogDatabase = mongoClient.getDatabase("cmpe283project1");

        userDAO = new UserDAO(blogDatabase);
        sessionDAO = new SessionDAO(blogDatabase);

        statsDAO= new StatsDAO(mongoClient.getDatabase("cmpe283project1"));
      //  AvailabiltyManager av =new AvailabiltyManager();
      //  av.start();

        Thread statsThread= new Thread(new statsCollector());
        statsThread.start();

        cfg = createFreemarkerConfiguration();
        this.si = null;
        this.rootFolder = null;

        si = new ServiceInstance(new URL(SJSULAB.getVmwareHostURL()), SJSULAB.getVmwareLogin(), SJSULAB.getVmwarePassword(), true);
        rootFolder = si.getRootFolder();

        setPort(8082);
        initializeRoutes();
        VMsDBCollection = mongoClient.getDatabase("cmpe283project1").getCollection("virtual_machines");
        if(VMsDBCollection != null)
        {
            System.out.println("VMs DB Collection found");
        }
        else
            System.out.println("Sorry VMs DB Collection NOT found");

    }

    abstract class FreemarkerBasedRoute extends Route {
        final Template template;

        /**
         * Constructor
         *
         * @param path The route path which is used for matching. (e.g. /hello, users/:name)
         */
        protected FreemarkerBasedRoute(final String path, final String templateName) throws IOException {
            super(path);
            template = cfg.getTemplate(templateName);
        }

        @Override
        public Object handle(Request request, Response response) {
            StringWriter writer = new StringWriter();
            try {
                doHandle(request, response, writer);
            } catch (Exception e) {
                e.printStackTrace();
                response.redirect("/internal_error");
            }
            return writer;
        }

        protected abstract void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException;

    }

    private void initializeRoutes() throws IOException {
        // this is the blog home page
        get(new FreemarkerBasedRoute("/", "blog_template.ftl") {
            @Override
            public void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request));

               // this is where we would normally load up the blog data
               // but this week, we just display a placeholder.
                HashMap<String, String> root = new HashMap<String, String>();

                template.process(root, writer);
            }
        });

        get(new FreemarkerBasedRoute("/display_vms", "display_vms.ftl") {
            @Override
            public void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                //String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request));
                List<Document> posts = new ArrayList<Document>();// = statsDAO.findByDateDescending(10);
                ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");

                SimpleHash root = new SimpleHash();
                System.out.println("We are good");
                BasicDBObject query = new BasicDBObject();
                VMsDBCollection.deleteMany(query);

                //HashMap<String, String> root = new HashMap<String, String>();
                if(!(mes == null || mes.length == 0)) {
                    for (int i = 0; i < mes.length; i++) {
                        Document text = new Document();
                        VirtualMachine vm = (VirtualMachine) mes[i];

                        if(!vm.getName().contains("Template")) {
                            String name = vm.getName();
                            text.append("name", name);
                            Document DB_VMs = new Document("_id", name);

                            if(vm.getConfig() != null) {
                                String instanceId = vm.getConfig().getInstanceUuid();
                                text.append("instanceId", instanceId);
                            }

                            System.out.println("VM Name : " + name);

                            String conectionState = vm.getRuntime().getConnectionState().toString();
                            text.append("conectionState", conectionState);
                            DB_VMs.append("conectionState", conectionState);

                            String ip = vm.getGuest().getIpAddress();
                            text.append("ip", ip);
                            DB_VMs.append("ip", ip);

                            String powerState = vm.getRuntime().getPowerState().toString();
                            text.append("powerState", powerState);

                            if (vm.getTriggeredAlarmState() == null) {
                                text.append("alarmState", "notTriggered");
                                DB_VMs.append("powerState", "notTriggered");
                            } else {
                                text.append("alarmState", "Triggered");
                                DB_VMs.append("powerState", "Triggered");
                            }

                            String launchTime = writeActualDate(vm.getRuntime().getBootTime());
                            text.append("launchTime", launchTime);
                            DB_VMs.append("launchTime", launchTime);

                            posts.add(text);
                            VMsDBCollection.insertOne(DB_VMs);

                        }
                    }
                }
                root.put("VMs", posts);
                template.process(root, writer);
            }
        });

        get(new FreemarkerBasedRoute("/create_vm", "create_vm.ftl") {
            @Override
            public void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                SimpleHash root = new SimpleHash();
                System.out.println("Inside Create VM backend");

                template.process(root, writer);
            }
        });

        post(new FreemarkerBasedRoute("/create_vm", "/create_vm.ftl") {
            @Override
            public void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {

                if (request.queryParams("Create") != null) {

                    ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");


                    //Clone VM
                    String vmname = request.queryParams("vmname");
                    String vm_template = request.queryParams("OS");

                    VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
                            rootFolder).searchManagedEntity("VirtualMachine", vm_template);

                    VirtualMachineRuntimeInfo vmri = vm.getRuntime();

                    HostSystem hs = new HostSystem(vm.getServerConnection(), vmri.getHost());

                    Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder).searchManagedEntity("Datacenter", "T03-DC");
                    ResourcePool rp = (ResourcePool) new InventoryNavigator(dc).searchManagedEntities("ResourcePool")[0];

                    if (vm == null) {
                        System.out.println("No VM found with name " + vm_template);

                        SimpleHash root = new SimpleHash();

                        root.put("login_error", "No template available");
                        template.process(root, writer);
                    } else {
                        try {
                            VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();

                            VirtualMachineRelocateSpec locateSpec = new VirtualMachineRelocateSpec();
                            locateSpec.setPool(rp.getMOR());
                            cloneSpec.setLocation(locateSpec);
                            cloneSpec.setPowerOn(false);
                            cloneSpec.setTemplate(false);

                            Task task = vm.cloneVM_Task((Folder) vm.getParent(),
                                    vm_template, cloneSpec);
                            System.out.println("Launching the VM clone task. " + "Please wait ...");
                            String status = task.waitForTask();
                            if (status == Task.SUCCESS) {
                                System.out.println("VM got cloned successfully.");
                            } else {
                                System.out.println("Failure -: VM cannot be cloned");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        response.redirect("/display_vms");
                    }
                } else if (request.queryParams("Cancle") != null) {

                    response.redirect("/display_vms");

                }
            }
        });



        post(new FreemarkerBasedRoute("/display_vms", "display_vms.ftl") {
            @Override
            public void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                //String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request));
                List<Document> posts = new ArrayList<Document>();// = statsDAO.findByDateDescending(10);
                SimpleHash root = new SimpleHash();

                BasicDBObject query = new BasicDBObject();
                ArrayList<String> VM_list = new ArrayList<String>();
                MongoCursor<Document> cursor = VMsDBCollection.find(query).iterator();

                while (cursor.hasNext()) {

                    String a = cursor.next().toJson();
                    System.out.println(a);

                    try {
                        JSONObject jsonObject = new JSONObject(a);
                        String vm_name = jsonObject.getString("_id");
                        VM_list.add(vm_name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                //take VM list from DB
                if (request.queryParams("PowerOn") != null) {
                    Iterator vm_iterator = VM_list.iterator();
                    while(vm_iterator.hasNext()) {
                        String VM_name = vm_iterator.next().toString();
                        boolean myCheckBox = request.queryParams(VM_name) != null;
                        if (myCheckBox) {
                            System.out.println("Power ON VM " + VM_name);
                            powerOn(VM_name);
                        }

                    }
                    response.redirect("/display_vms");

                } else if (request.queryParams("PowerOff") != null) {
                    Iterator vm_iterator = VM_list.iterator();
                    while(vm_iterator.hasNext()) {
                        String VM_name = vm_iterator.next().toString();
                        boolean myCheckBox = request.queryParams(VM_name) != null;
                        if (myCheckBox) {
                            System.out.println("Power Off VM " + VM_name);
                            powerOff(VM_name);
                        }
                    }
                    response.redirect("/display_vms");

                } else if (request.queryParams("Delete") != null) {
                    Iterator vm_iterator = VM_list.iterator();
                    while(vm_iterator.hasNext()) {
                        String VM_name = vm_iterator.next().toString();
                        boolean myCheckBox = request.queryParams(VM_name) != null;
                        if (myCheckBox)
                            System.out.println("Delete VM "+ VM_name);
                            deleteVM(VM_name);
                    }
                    response.redirect("/display_vms");

                } else if (request.queryParams("Get_Chart") != null) {
                Iterator vm_iterator = VM_list.iterator();
                while(vm_iterator.hasNext()) {
                    String VM_name = vm_iterator.next().toString();
                    boolean myCheckBox = request.queryParams(VM_name) != null;
                    if (myCheckBox) {
                        System.out.println("Get VM " + VM_name +"Charts" );
                        CurrentSelectedVM = VM_name;
                    }
                }
                response.redirect("/gChart");

            } else if (request.queryParams("Create") != null) {
                    response.redirect("/create_vm");

                }else {
                    System.out.println("Invalid ");
                    response.redirect("/display_vms");
                    // ???
                }

            }
        });

        // google chart handler
        get(new FreemarkerBasedRoute("/gChart", "GoogleLine.ftl") {
            @Override
            public void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request));

                if (CurrentSelectedVM != null) {

                    ArrayList<ArrayList> gchartData = statsDAO.getGchart(CurrentSelectedVM);
                    SimpleHash root = new SimpleHash();
                    root.put("VMName", CurrentSelectedVM);
                    CurrentSelectedVM = null;

                    root.put("gcdata", gchartData);
                    //System.out.println(gchartData);
                    // System.out.println(gchartData.get(0));

                    template.process(root, writer);
                }

            }
        });

        post(new FreemarkerBasedRoute("/gChart", "GoogleLine.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {

                if (request.queryParams("Home") != null) {
                    response.redirect("/display_vms");
                }
            }
        });


        // handle the signup post
        post(new FreemarkerBasedRoute("/signup", "signup.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String email = request.queryParams("email");
                String username = request.queryParams("username");
                String password = request.queryParams("password");
                String verify = request.queryParams("verify");

                HashMap<String, String> root = new HashMap<String, String>();
                root.put("username", StringEscapeUtils.escapeHtml4(username));
                root.put("email", StringEscapeUtils.escapeHtml4(email));

                if (validateSignup(username, password, verify, email, root)) {
                    // good user
                    System.out.println("Signup: Creating user with: " + username + " " + password);
                    if (!userDAO.addUser(username, password, email)) {
                        // duplicate user
                        root.put("username_error", "Username already in use, Please choose another");
                        template.process(root, writer);
                    }
                    else {
                        // good user, let's start a session
                        String sessionID = sessionDAO.startSession(username);
                        System.out.println("Session ID is" + sessionID);

                        response.raw().addCookie(new Cookie("session", sessionID));
                        response.redirect("/login");
                    }
                }
                else {
                    // bad signup
                    System.out.println("User Registration did not validate");
                    template.process(root, writer);
                }
            }
        });

        // present signup form for blog
        get(new FreemarkerBasedRoute("/signup", "signup.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer)
                    throws IOException, TemplateException {

                SimpleHash root = new SimpleHash();

                // initialize values for the form.
                root.put("username", "");
                root.put("password", "");
                root.put("email", "");
                root.put("password_error", "");
                root.put("username_error", "");
                root.put("email_error", "");
                root.put("verify_error", "");

                template.process(root, writer);
            }
        });




        get(new FreemarkerBasedRoute("/welcome", "welcome.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {

                String cookie = getSessionCookie(request);
                String username = sessionDAO.findUserNameBySessionId(cookie);

                if (username == null) {
                    System.out.println("welcome() can't identify the user, redirecting to signup");
                    response.redirect("/signup");

                }
                else {
                    SimpleHash root = new SimpleHash();

                    root.put("username", username);

                    template.process(root, writer);
                }
            }
        });


        // present the login page
        get(new FreemarkerBasedRoute("/login", "login.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                SimpleHash root = new SimpleHash();

                root.put("username", "");
                root.put("login_error", "");

                template.process(root, writer);
            }
        });


        // process output coming from login form. On success redirect folks to the welcome page
        // on failure, just return an error and let them try again.
        post(new FreemarkerBasedRoute("/login", "login.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {

                String username = request.queryParams("username");
                String password = request.queryParams("password");

                System.out.println("Login: User submitted: " + username + "  " + password);

                Document user = userDAO.validateLogin(username, password);

                if (user != null) {

                    // valid user, let's log them in
                    String sessionID = sessionDAO.startSession(user.get("_id").toString());

                    if (sessionID == null) {
                        response.redirect("/internal_error");
                    }
                    else {
                        // set the cookie for the user's browser
                        response.raw().addCookie(new Cookie("session", sessionID));

                        response.redirect("/display_vms");
                    }
                }
                else {
                    SimpleHash root = new SimpleHash();


                    root.put("username", StringEscapeUtils.escapeHtml4(username));
                    root.put("password", "");
                    root.put("login_error", "Invalid Login");
                    template.process(root, writer);
                }
            }
        });



        // allows the user to logout of the blog
        get(new FreemarkerBasedRoute("/logout", "signup.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {

                String sessionID = getSessionCookie(request);

                if (sessionID == null) {
                    // no session to end
                    response.redirect("/login");
                }
                else {
                    // deletes from session table
                    sessionDAO.endSession(sessionID);

                    // this should delete the cookie
                    Cookie c = getSessionCookieActual(request);
                    c.setMaxAge(0);

                    response.raw().addCookie(c);

                    response.redirect("/login");
                }
            }
        });


        // used to process internal errors
        get(new FreemarkerBasedRoute("/internal_error", "error_template.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                SimpleHash root = new SimpleHash();

                root.put("error", "System has encountered an error.");
                template.process(root, writer);
            }
        });
    }

    // helper function to get session cookie as string
    private String getSessionCookie(final Request request) {
        if (request.raw().getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.raw().getCookies()) {
            if (cookie.getName().equals("session")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String writeActualDate(Calendar cal){
        if(cal != null) {
            Date creationDate = cal.getTime();
            SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm");
            return (date_format.format(creationDate));
        }
        else
            return "";
    }

    public void powerOn(String vmname){
        try {
            VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
                    rootFolder).searchManagedEntity("VirtualMachine", vmname);
            System.out.println("Powering on virtual machine '"+vm.getName() +"'. Please wait...");
            Task t=vm.powerOnVM_Task(null);
            if(t.waitForTask()== Task.SUCCESS)
            {
                System.out.println("Virtual machine powered on.");

            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();;
        }
    }

    public void powerOff(String vmname){
        try{
            VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
                    rootFolder).searchManagedEntity("VirtualMachine", vmname);
            System.out.println("Powering off virtual machine '"+vm.getName() +"'. Please wait...");
            Task t = vm.powerOffVM_Task();
            if(t.waitForTask()== Task.SUCCESS)
            {
                System.out.println("Virtual machine powered off.");
            }

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void deleteVM(String vmname) throws RemoteException {
        if(vmname != null){
            try {
                VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
                    rootFolder).searchManagedEntity("VirtualMachine", vmname);

                Task task = vm.destroy_Task();
                if(task.waitForTask() == Task.SUCCESS){
                    System.out.println("VM deletion successful");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // helper function to get session cookie as string
    private Cookie getSessionCookieActual(final Request request) {
        if (request.raw().getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.raw().getCookies()) {
            if (cookie.getName().equals("session")) {
                return cookie;
            }
        }
        return null;
    }

    // tags the tags string and put it into an array
    private ArrayList<String> extractTags(String tags) {

        // probably more efficent ways to do this.
        //
        // whitespace = re.compile('\s')

        tags = tags.replaceAll("\\s", "");
        String tagArray[] = tags.split(",");

        // let's clean it up, removing the empty string and removing dups
        ArrayList<String> cleaned = new ArrayList<String>();
        for (String tag : tagArray) {
            if (!tag.equals("") && !cleaned.contains(tag)) {
                cleaned.add(tag);
            }
        }

        return cleaned;
    }

    // validates that the registration form has been filled out right and username conforms
    public boolean validateSignup(String username, String password, String verify, String email,
                                  HashMap<String, String> errors) {
        String USER_RE = "^[a-zA-Z0-9_-]{3,20}$";
        String PASS_RE = "^.{3,20}$";
        String EMAIL_RE = "^[\\S]+@[\\S]+\\.[\\S]+$";

        errors.put("username_error", "");
        errors.put("password_error", "");
        errors.put("verify_error", "");
        errors.put("email_error", "");

        if (!username.matches(USER_RE)) {
            errors.put("username_error", "invalid username. try just letters and numbers");
            return false;
        }

        if (!password.matches(PASS_RE)) {
            errors.put("password_error", "invalid password.");
            return false;
        }


        if (!password.equals(verify)) {
            errors.put("verify_error", "password must match");
            return false;
        }

        if (!email.equals("")) {
            if (!email.matches(EMAIL_RE)) {
                errors.put("email_error", "Invalid Email Address");
                return false;
            }
        }

        return true;
    }

    private Configuration createFreemarkerConfiguration() {
        Configuration retVal = new Configuration();
        retVal.setClassForTemplateLoading (BlogController.class, "/freemarker");
        return retVal;
    }
}
