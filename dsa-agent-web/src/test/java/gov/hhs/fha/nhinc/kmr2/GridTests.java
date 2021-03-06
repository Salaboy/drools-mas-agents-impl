/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.hhs.fha.nhinc.kmr2;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.Persistence;

import org.apache.commons.io.IOUtils;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactoryService;
import org.drools.SystemEventListenerFactory;
import org.drools.agent.KnowledgeAgent;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactoryService;
import org.drools.builder.ResourceType;
import org.drools.conf.AssertBehaviorOption;
import org.drools.grid.*;
import org.drools.grid.conf.GridPeerServiceConfiguration;
import org.drools.grid.conf.impl.GridPeerConfiguration;
import org.drools.grid.helper.GridHelper;
import org.drools.grid.impl.GridImpl;
import org.drools.grid.impl.MultiplexSocketServerImpl;
import org.drools.grid.io.impl.MultiplexSocketServiceCongifuration;
import org.drools.grid.remote.mina.MinaAcceptorFactoryService;
import org.drools.grid.service.directory.WhitePages;
import org.drools.grid.service.directory.impl.CoreServicesLookupConfiguration;
import org.drools.grid.service.directory.impl.JpaWhitePages;
import org.drools.grid.service.directory.impl.WhitePagesLocalConfiguration;
import org.drools.grid.timer.impl.CoreServicesSchedulerConfiguration;
import org.drools.io.Resource;
import org.drools.io.impl.ByteArrayResource;
import org.drools.io.impl.ChangeSetImpl;
import org.drools.io.impl.ClassPathResource;
import org.drools.io.internal.InternalResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author salaboy
 */
public class GridTests {
    
    public GridTests() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
     private Map<String, GridServiceDescription> coreServicesMap;
    protected Grid grid1;
    
    protected GridNode remoteN1;
    private Server server;
    
    @Before
    public void setUp() {
         DeleteDbFiles.execute("~", "mydb", false);

        System.out.println("Staring DB for white pages ...");
        
        try {
            
            server = Server.createTcpServer(new String[] {"-tcp","-tcpAllowOthers","-tcpDaemon","-trace"}).start(); 
        } catch (SQLException ex) {
            System.out.println("ERROR: "+ex.getMessage());
            
        }
        System.out.println("DB for white pages started! ");

        this.coreServicesMap = new HashMap();
        createRemoteNode();
    }

    @After
    public void tearDown() {
        remoteN1.dispose();
        grid1.get(SocketService.class).close();
        server.stop();
        
    }
    
    private void createRemoteNode(){
        grid1 = new GridImpl("peer1", new HashMap<String, Object>() );
        configureGrid1( grid1,
                        8000,
                        new JpaWhitePages(Persistence.createEntityManagerFactory("org.drools.grid")) );

        Grid grid2 = new GridImpl("peer2", new HashMap<String, Object>() );
        configureGrid1( grid2,
                        -1,
                        grid1.get( WhitePages.class ) );

        GridNode n1 = grid1.createGridNode( "n1" );
        grid1.get( SocketService.class ).addService( "n1", 8000, n1 );
               
        GridServiceDescription<GridNode> n1Gsd = grid2.get( WhitePages.class ).lookup( "n1" );
        GridConnection<GridNode> conn = grid2.get( ConnectionFactoryService.class ).createConnection( n1Gsd );
        remoteN1 = conn.connect();
        
        
    
    }
    
    private void configureGrid1(Grid grid,
                                int port,
                                WhitePages wp) {

        //Local Grid Configuration, for our client
        GridPeerConfiguration conf = new GridPeerConfiguration();

        //Configuring the Core Services White Pages
        GridPeerServiceConfiguration coreSeviceWPConf = new CoreServicesLookupConfiguration( coreServicesMap );
        conf.addConfiguration( coreSeviceWPConf );

        //Configuring the Core Services Scheduler
        GridPeerServiceConfiguration coreSeviceSchedulerConf = new CoreServicesSchedulerConfiguration();
        conf.addConfiguration( coreSeviceSchedulerConf );

        //Configuring the WhitePages 
        WhitePagesLocalConfiguration wplConf = new WhitePagesLocalConfiguration();
        wplConf.setWhitePages( wp );
        conf.addConfiguration( wplConf );

//        //Create a Local Scheduler
//        SchedulerLocalConfiguration schlConf = new SchedulerLocalConfiguration( "myLocalSched" );
//        conf.addConfiguration( schlConf );
        
        if ( port >= 0 ) {
            //Configuring the SocketService
            MultiplexSocketServiceCongifuration socketConf = new MultiplexSocketServiceCongifuration( new MultiplexSocketServerImpl( "127.0.0.1",
                                                                                                                              new MinaAcceptorFactoryService(),
                                                                                                                              SystemEventListenerFactory.getSystemEventListener(),
                                                                                                                              grid) );
            
            socketConf.addService( WhitePages.class.getName(), wplConf.getWhitePages(), port );
            
//            socketConf.addService( SchedulerService.class.getName(), schlConf.getSchedulerService(), port );
                        
            conf.addConfiguration( socketConf );
        }
        conf.configure( grid );
        
        

    }
    
    protected StatefulKnowledgeSession createSession(){
        KnowledgeBuilder kbuilder = remoteN1.get( KnowledgeBuilderFactoryService.class ).newKnowledgeBuilder();

        assertNotNull( kbuilder );

         String rule = "package test\n"
                 
                 
                 + "rule \"test\""
                 + "  when"
                 + "       $o: Object()"
                 + "  then"
                 + "      System.out.println(\"My Object -> \"+$o);"
                 + " end";

        kbuilder.add( new ByteArrayResource( rule.getBytes() ),
                      ResourceType.DRL );

        KnowledgeBuilderErrors errors = kbuilder.getErrors();
        if ( errors != null && errors.size() > 0 ) {
            for ( KnowledgeBuilderError error : errors ) {
                System.out.println( "Error: " + error.getMessage() );

            }
            fail("KnowledgeBase did not build");
        }
        KnowledgeBaseConfiguration kbaseConf = remoteN1.get( KnowledgeBaseFactoryService.class ).newKnowledgeBaseConfiguration();
        kbaseConf.setProperty(AssertBehaviorOption.PROPERTY_NAME, "equality");
        KnowledgeBase kbase = remoteN1.get( KnowledgeBaseFactoryService.class ).newKnowledgeBase(kbaseConf);

        assertNotNull( kbase );

        kbase.addKnowledgePackages( kbuilder.getKnowledgePackages() );

        StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession();
        
        remoteN1.set("ksession-rules", session);
        
        return session;
    
    }

   
   
    
    
      @Test
    
    public void remoteKAgentRealChangeSetTest() throws InterruptedException {
        StatefulKnowledgeSession ksession = createSession();
        
        

        int fired = ksession.fireAllRules();
        Assert.assertEquals(1, fired);

       
        
         String changeSetString = "<change-set xmlns='http://drools.org/drools-5.0/change-set'>"
                 + "<add>"
                 + "    <resource type='DRL' source='classpath:org/drools/mas/acl_common.drl' />"
                 + "    <resource type='CHANGE_SET' source='classpath:org/drools/informer/informer-changeset.xml' />"
                 + "    <resource type='CHANGE_SET' source='classpath:org/drools/informer/interaction/interaction-changeset.xml' />"
                 + "    <resource type='DRL' source='classpath:gov/hhs/fha/nhinc/kmr2/clinicalAgent/declares/risk_declares.drl' />"
                 + "    <resource type='DRL' source='classpath:gov/hhs/fha/nhinc/kmr2/clinicalAgent/declares/dxGuide_declares.drl' />"
                 + "    <resource type='CHANGE_SET' source='classpath:gov/hhs/fha/nhinc/kmr2/clinicalAgent/test/risk_model_list.xml' />"
                 + "    <resource type='CHANGE_SET' source='classpath:gov/hhs/fha/nhinc/kmr2/clinicalAgent/test/diag_model_list.xml' />"
                 + "    <resource type='CHANGE_SET' source='classpath:config/services_changeset.xml' />"
                 + "    <resource type='DRL' source='classpath:gov/hhs/fha/nhinc/kmr2/clinicalAgent/default/subsession_default.drl' />"
                 + "    <resource type='DRL' source='classpath:gov/hhs/fha/nhinc/kmr2/clinicalAgent/mock/mock_ptsd_decision.drl'"
                 + "            name='MockDecision' description='Post Traumatic Stress Disorder' />"
                 + "    <resource type='DRL' source='classpath:gov/hhs/fha/nhinc/kmr2/clinicalAgent/default/usecase_1.drl' />"
                 + "</add>"
                 + "</change-set>";
        Resource changeSetRes = new ByteArrayResource(changeSetString.getBytes());
        ((InternalResource) changeSetRes).setResourceType(ResourceType.CHANGE_SET);
        
        
        KnowledgeAgent kAgent = GridHelper.getKnowledgeAgentRemoteClient(remoteN1.getId(), "ksession-rules");
        kAgent.applyChangeSet(changeSetRes);

        Thread.sleep(25000);

        
        fired = ksession.fireAllRules();
        
        Assert.assertEquals(0, fired);


    }
}
