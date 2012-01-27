/*
 * TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
 * Copyright (c) 2008, Nationwide Health Information Network (NHIN) Connect.
 * All rights reserved.Redistribution and use in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of the NHIN Connect Project nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *    END OF TERMS AND CONDITIONS
 */

package gov.hhs.fha.nhinc.kmr2;

import org.drools.ClassObjectFilter;
import org.drools.definition.type.FactType;
import org.drools.mas.ACLMessage;
import org.drools.mas.Act;
import org.drools.mas.Encodings;
import org.drools.mas.body.acts.Inform;
import org.drools.mas.core.DroolsAgent;
import org.drools.mas.core.DroolsAgentConfiguration;
import org.drools.mas.core.DroolsAgentFactory;
import org.drools.mas.core.DroolsAgentResponseInformer;
import org.drools.mas.util.ACLMessageFactory;
import org.drools.mas.util.MessageContentFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

public class TestAgent {



    private static DroolsAgent mainAgent;

    private static MockResponseInformer mainResponseInformer;


    private ACLMessageFactory factory = new ACLMessageFactory( Encodings.XML );


    @BeforeClass
    public static void createAgents() {

        mainResponseInformer = new MockResponseInformer();

        DroolsAgentConfiguration mainConfig = new DroolsAgentConfiguration();
        mainConfig.setAgentId( "Mock Test Agent" );
        mainConfig.setChangeset("config/agent_changeset.xml");
        mainConfig.setDefaultSubsessionChangeSet("config/subsession_default.xml");
        mainConfig.setResponseInformer( mainResponseInformer );
        DroolsAgentConfiguration.SubSessionDescriptor subDescr1 = new DroolsAgentConfiguration.SubSessionDescriptor(
                "sessionSurvey",
                "gov/hhs/fha/nhinc/kmr2/clinicalAgent/test/survey_test.xml",
                "NOT_USED_YET" );
        mainConfig.addSubSession(subDescr1);
        mainAgent = DroolsAgentFactory.getInstance().spawn( mainConfig );

    }



    @AfterClass
    public static void cleanUp() {
        if (mainAgent != null) {
            mainAgent.dispose();
        }

    }


    private void sleep( long millis ) {
        try {
            Thread.sleep( millis );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }









    private String probe( String patientId ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.put("patientId", patientId);

        ACLMessage req = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("probe", args) );
        mainAgent.tell(req);
        ACLMessage ans = mainResponseInformer.getResponses(req).get(1);
        return ret(ans);
    }

    private String ret(ACLMessage ans) {
        if ( ! ans.getPerformative().equals( Act.INFORM ) ) {
            System.err.println( ans );
        }
        assertEquals( Act.INFORM, ans.getPerformative() );
        return ((Inform) ans.getBody()).getProposition().getEncodedContent();
    }


    private String getSurvey( String userId, String patientId, String surveyId ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.put("userId",userId);
        args.put("patientId",patientId);
        args.put("surveyId",surveyId);

        ACLMessage req = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("getSurvey", args));
        mainAgent.tell(req);
        ACLMessage ans = mainResponseInformer.getResponses(req).get(1);
        return ret(ans);
    }

    private String setSurvey( String userId, String patientId, String surveyId, String questionId, String value ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.put("userId",userId);
        args.put("patientId",patientId);
        args.put("surveyId",surveyId);
        args.put("questionId", questionId);
        args.put("answer",value);

        ACLMessage set = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("setSurvey", args) );
        mainAgent.tell(set);
        ACLMessage ans = mainResponseInformer.getResponses(set).get(1);
        return ret(ans);
    }

    private void setRiskThreshold(String userId, String patientId, String modelId, String type, Integer value) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.clear();
        args.put("userId",userId);
        args.put("patientId",patientId);
        args.put("modelId",modelId);
        args.put("type",type);
        args.put("threshold",value);
        ACLMessage setThold = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("setRiskThreshold", args) );
        mainAgent.tell(setThold);
    }

    private String getModels( String userId, String patientId, List tags ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.put("userId",userId);
        args.put("patientId",patientId);
        args.put("types", tags);
        ACLMessage req = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("getModels", args));
        mainAgent.tell(req);
        ACLMessage ans = mainResponseInformer.getResponses(req).get(1);


        return ret(ans);
    }

    private String getRiskModels( String userId, String patientId ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.put("userId",userId);
        args.put("patientId",patientId);
        ACLMessage req = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("getRiskModels", args));
        mainAgent.tell(req);
        ACLMessage ans = mainResponseInformer.getResponses(req).get(1);


        return ret(ans);
    }

    private String getRiskModesDetail(String userId, String patientId, String[] modelsIds) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();


        args.put("userId", userId );
        args.put("patientId", patientId );
        args.put("modelIds",modelsIds );

        ACLMessage req = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("getRiskModelsDetail", args) );

        mainAgent.tell(req);

        ACLMessage ans = mainResponseInformer.getResponses(req).get(1);
        return ret( ans );
    }


    public String startDiagnosticGuideProcess( String userId, String patientId, String disease ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();

        args.put("userId", userId );
        args.put("patientId", patientId );
        args.put("disease", disease );
        ACLMessage start = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("startDiagnosticGuideProcess", args) );

        mainAgent.tell(start);
        ACLMessage ans = mainResponseInformer.getResponses(start).get(1);
//        MessageContentEncoder.decodeBody( ans.getBody(), Encodings.XML );
//        String dxProcessId = (String) ((Inform) ans.getBody()).getProposition().getData();
//        return dxProcessId;
        return ret(ans);
    }

    public String getDiagnosticProcessStatus( String userId, String patientId, String dxProcessId, boolean forceRefresh ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.put("userId", userId);
        args.put("patientId", patientId );
        args.put("dxProcessId",dxProcessId);
        args.put("forceRefresh",forceRefresh );
        ACLMessage reqStatus = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("getDiagnosticProcessStatus", args) );

        mainAgent.tell(reqStatus);
        ACLMessage ans2 = mainResponseInformer.getResponses(reqStatus).get(1);
        return ret( ans2 );
    }

    public String setDiagnosticActionStatus(String userId, String patientId, String dxProcessId, String actionId, String status) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.clear();
        args.put("userId", userId);
        args.put("patientId", patientId );
        args.put("dxProcessId",dxProcessId);
        args.put("actionId",actionId);
        args.put("status",status);

        ACLMessage reqAction = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("setDiagnosticActionStatus", args) );
        mainAgent.tell(reqAction);
        ACLMessage ans = mainResponseInformer.getResponses(reqAction).get(1);
        return ret( ans ).replaceAll("<string>","").replaceAll("</string>","") ;

    }

    public void advanceDiagnosticProcessStatus( String userId, String patientId, String dxProcessId ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.put("userId", userId);
        args.put("patientId", patientId );
        args.put("dxProcessId",dxProcessId);
        ACLMessage reqStatus = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("advanceDiagnosticGuideProcess", args) );

        mainAgent.tell(reqStatus);
//           ACLMessage ans2 = mainResponseInformer.getResponses(reqStatus).get(1);

    }


    public void completeDiagnosticGuideProcess( String userId, String patientId, String dxProcessId, String status ) {
        Map<String,Object> args = new LinkedHashMap<String,Object>();
        args.put("userId", userId);
        args.put("patientId", patientId );
        args.put("dxProcessId",dxProcessId);
        args.put("status", status);
        ACLMessage reqStatus = factory.newRequestMessage("me","you", MessageContentFactory.newActionContent("completeDiagnosticGuideProcess", args) );

        mainAgent.tell(reqStatus);
//           ACLMessage ans2 = mainResponseInformer.getResponses(reqStatus).get(1);

    }



    private List<String> getModels( String xml ) {
        return getElements( xml, "//modelId" );
    }


    public List<String> getElements(String xml, String xpath) {
        try {
            Document dox = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new ByteArrayInputStream( xml.getBytes()) );
            XPath finder = XPathFactory.newInstance().newXPath();

            NodeList nodes = (NodeList) finder.evaluate( xpath, dox, XPathConstants.NODESET );
            List<String> list = new ArrayList<String>();
            for ( int j = 0; j < nodes.getLength(); j++ ) {
                list.add(((Element) nodes.item(j)).getTextContent());
            }

            return list;
        } catch (Exception e) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        return Collections.emptyList();
    }

    public String getValue(String xml, String xpath) {
        try {
            Document dox = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new ByteArrayInputStream( xml.getBytes()) );
            XPath finder = XPathFactory.newInstance().newXPath();

            return (String) finder.evaluate( xpath, dox, XPathConstants.STRING );
        } catch (Exception e) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        return null;
    }

    private static final String testDecModel = "gov.hhs.fha.nhinc.kmr2.clinicalAgent.models.decision";





























    @Test
    // @Ignore
    public void testSetSurvey() {
        String[] qid = new String[8];
        String[] values = new String[8];
        XPath finder = XPathFactory.newInstance().newXPath();


        String xmlSurv = getSurvey( "drX", "99990070", "123456UNIQUESURVEYID" );


        for ( int j = 1; j < 8; j++ ) {
            qid[j] = getValue( xmlSurv, "//org.drools.informer.presentation.QuestionGUIAdapter/questionName[.='question"+j+"']/../itemId" );
            System.out.println("ID OF Q" + j + " >> " + qid[j] );
        }


        for ( int j = 1; j < 8; j++ ) {
            String val = getValue( xmlSurv, "//org.drools.informer.presentation.QuestionGUIAdapter/itemId[.='"+qid[j]+"']/../successType" );
            switch (j) {
                case 4  : assertEquals( "valid", val ); break;
                case 7  : assertEquals( "missing", val ); break;
                default : assertEquals( "invalid", val ); break;
            }
        }


        values[1] = new Integer((int) Math.ceil( 6 * Math.random() )).toString();
        values[2] = Math.random() > 0.5 ? "Urban" : "Rural";
        List v3 = new ArrayList();
        if ( Math.random() > 0.5 ) { v3.add( "CMP" ); }
        if ( Math.random() > 0.5 ) { v3.add( "FSH" ); }
        if ( Math.random() > 0.5 ) { v3.add( "GLF" ); }
        if ( Math.random() > 0.5 ) { v3.add( "HGL" ); }
        if ( v3.size() == 0 ) {
            v3.add( "HGL" );
        }
        values[3] = v3.toString().replace("[","").replace("]","").replace(" ","");
        values[4] = new Integer((int) Math.round( 100 * Math.random() )).toString();
        values[5] = UUID.randomUUID().toString();
        values[6] = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        values[7] = new Integer((int) Math.round( 100 * Math.random() )).toString();


        for ( int j = 1; j < 8; j++ ) {
            setSurvey( "drx", "99990070", "123456UNIQUESURVEYID", qid[j], values[j].toString());
        }


        xmlSurv = getSurvey( "drX", "99990070", "123456UNIQUESURVEYID" );
        for ( int j = 1; j < 8; j++ ) {
            String val = getValue( xmlSurv, "//org.drools.informer.presentation.QuestionGUIAdapter/itemId[.='"+qid[j]+"']/../successType" );
            switch (j) {
                default : assertEquals( "valid", val ); break;
            }
        }



        for ( int j = 1; j < 8; j++ ) {
            String val = getValue( xmlSurv, "//org.drools.informer.presentation.QuestionGUIAdapter/itemId[.='"+qid[j]+"']/../currentAnswer" );
            assertEquals( val, values[j].toString() );
        }


        for ( int j = 1; j < 8; j++ ) {
            setSurvey( "drx", "99990070", "123456UNIQUESURVEYID", qid[j], null);
        }

        xmlSurv = getSurvey( "drX", "99990070", "123456UNIQUESURVEYID" );
        for ( int j = 1; j < 8; j++ ) {
            String val = getValue( xmlSurv, "//org.drools.informer.presentation.QuestionGUIAdapter/itemId[.='"+qid[j]+"']/../successType" );
            switch (j) {
                case 4  :
                case 7  : assertEquals( "missing", val ); break;
                default : assertEquals( "invalid", val ); break;
            }
        }


    }





    @Test
        // @Ignore
        public void testExceedAndReset() {

            List<String> modelsIds = getElements(getRiskModels("docX", "patient33"), "//modelId");
            String modelStats = getRiskModesDetail( "docX", "patient33", modelsIds.toArray(new String[modelsIds.size()]) );


            FactType alertType = mainAgent.getInnerSession("patient33").getKnowledgeBase().getFactType("org.drools.informer.interaction", "Alert");
            Class alertClass = alertType.getFactClass();
            Collection alerts = mainAgent.getInnerSession("patient33").getObjects( new ClassObjectFilter(alertClass) );
            assertEquals( 0, alerts.size() );



            String sid1 = getValue( modelStats, "//modelId[.='MockPTSD']/../surveyId" );
            assertNotNull( sid1 );

            String ptsdSurvey = getSurvey( "docX", "patient33", sid1);

            String gender = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Gender']/../itemId" );
            String deployments = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Deployments']/../itemId" );
            String alcohol = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Alcohol']/../itemId" );
            String age = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Age']/../itemId" );

            assertNotNull( gender );
            assertNotNull( deployments );
            assertNotNull( age );

            setSurvey( "drX", "patient33", sid1, deployments, "1" );
            setSurvey( "drX", "patient33", sid1, gender, "female" );
            setSurvey( "drX", "patient33", sid1, alcohol, "yes" );
            setSurvey( "drX", "patient33", sid1, age, "30" );

            modelStats = getRiskModesDetail( "docX", "patient33", modelsIds.toArray(new String[modelsIds.size()]) );
            assertEquals( "30", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );


            setRiskThreshold( "drX", "patient33", "MockPTSD", "Alert", 25 );


            assertEquals( 2, alerts.size() );
            sleep(12000);
            alerts = mainAgent.getInnerSession("patient33").getObjects( new ClassObjectFilter(alertClass) );
            assertEquals( 0, alerts.size() );


            for ( Object o :  mainAgent.getInnerSession("patient33").getObjects() )
                System.err.println( o );





            setSurvey( "drX", "patient33", sid1, age, "1" );

            modelStats = getRiskModesDetail( "docX", "patient33", modelsIds.toArray(new String[modelsIds.size()]) );
            assertEquals( "16", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );


            setSurvey( "drX", "patient33", sid1, age, "40" );

            modelStats = getRiskModesDetail( "docX", "patient33", modelsIds.toArray(new String[modelsIds.size()]) );
            assertEquals( "35", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );






            FactType xType = mainAgent.getInnerSession("patient33").getKnowledgeBase().getFactType("org.drools.informer.interaction", "TicketActor");
            Collection zalerts = mainAgent.getInnerSession("patient33").getObjects( new ClassObjectFilter(xType.getFactClass()) );
            for (Object o : zalerts) {
                System.err.println(o);
            }


            alerts = mainAgent.getInnerSession("patient33").getObjects( new ClassObjectFilter(alertClass) );
            for (Object o : alerts) {
                System.err.println(o);
            }
            assertEquals( 2, alerts.size() );



        }









    @Test
    // @Ignore
    public void testSetAndResetAndSet() {

        String[] mids = new String[] { "MockPTSD" };

        String modelStats = getRiskModesDetail( "docX", "patient33", mids );

        String sid1 = getValue( modelStats, "//modelId[.='MockPTSD']/../surveyId" );
        assertNotNull( sid1 );

        String ptsdSurvey = getSurvey( "docX", "patient33", sid1);

        String gender = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Gender']/../itemId" );
        String deployments = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Deployments']/../itemId" );
        String alcohol = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Alcohol']/../itemId" );
        String age = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Age']/../itemId" );

        assertNotNull( gender );
        assertNotNull( deployments );
        assertNotNull( age );

        String set;
        set = setSurvey( "drX", "patient33", sid1, deployments, "1" );
        System.out.println( set );
        set = setSurvey( "drX", "patient33", sid1, gender, "female" );
        System.out.println( set );
        set = setSurvey( "drX", "patient33", sid1, alcohol, "yes" );
        System.out.println( set );
        set = setSurvey( "drX", "patient33", sid1, age, "30" );
        System.out.println( set );

        modelStats = getRiskModesDetail( "docX", "patient33", mids );
        assertEquals( "30", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );
        assertEquals( "Average", getValue( modelStats, "//modelId[.='MockPTSD']/../severity" ) );




        set = setSurvey( "drX", "patient33", sid1, age, null );
        System.out.println( set );
        set = setSurvey( "drX", "patient33", sid1, gender, null );
        System.out.println( set );

        modelStats = getRiskModesDetail( "docX", "patient33", mids );
        assertEquals( "-1", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );
        assertEquals( "n/a", getValue( modelStats, "//modelId[.='MockPTSD']/../severity" ) );



        set = setSurvey( "drX", "patient33", sid1, age, "25" );
        modelStats = getRiskModesDetail( "docX", "patient33", mids );
        assertEquals( "-1", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );
        assertEquals( "n/a", getValue( modelStats, "//modelId[.='MockPTSD']/../severity" ) );



        set = setSurvey( "drX", "patient33", sid1, gender, "male" );
        modelStats = getRiskModesDetail( "docX", "patient33", mids );
        assertEquals( "38", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );
        assertEquals( "Average", getValue( modelStats, "//modelId[.='MockPTSD']/../severity" ) );



        set = setSurvey( "drX", "patient33", sid1, age, "30" );
        modelStats = getRiskModesDetail( "docX", "patient33", mids );
        assertEquals( "40", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );
        assertEquals( "Average", getValue( modelStats, "//modelId[.='MockPTSD']/../severity" ) );


    }








    @Test
    // @Ignore
    public void testGetModels() {

        String diagModels = getModels("drX", "patient33", Arrays.asList("Diagnostic") );

        System.out.println( diagModels );

        List<String> diagList = getModels( diagModels );
        assertEquals(1, diagList.size());
        assertTrue( diagList.containsAll( Arrays.asList("MockDiag" ) ) );



        setRiskThreshold( "drX", "patient33", "MockPTSD", "Display", 35 );

        String riskModels = getRiskModels("drX", "patient33");

        System.err.println( riskModels );

        List<String> riskList = getModels( riskModels );
        assertEquals( 3, riskList.size());
        assertTrue( riskList.containsAll( Arrays.asList( "MockPTSD", "MockCold", "MockDiabetes" ) ) );

        assertEquals( "35", getValue( riskModels, "//modelId[.='MockPTSD']/../displayThreshold" ) );


        String enterpriseRiskModels = getModels("drX", "patient33", Arrays.asList("E", "Risk") );

        System.out.println( enterpriseRiskModels );

        List<String> enterpriseRiskModelsList = getModels( enterpriseRiskModels );
        assertEquals(2, enterpriseRiskModelsList.size());
        assertTrue( enterpriseRiskModelsList.containsAll( Arrays.asList( "MockPTSD", "MockCold" ) ) );


    }




    @Test
    // @Ignore
    public void testComplexDiagnosticAction() {


        Map<String,Object> args = new LinkedHashMap<String,Object>();

        String dxProcessReturn = startDiagnosticGuideProcess( "docX", "patient33", "Post Traumatic Stress Disorder");
        String dxProcessId = getValue( dxProcessReturn, "//dxProcessId" );

        assertNotNull( dxProcessId );

        String statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );


        System.err.println(statusXML);


        String testActionId = getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/actionId" );
        String testQuestId = getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/questionnaireId" );
        assertNotNull( testActionId );
        assertNotNull( testQuestId );

        String stat1 = setDiagnosticActionStatus( "drX", "patient33", dxProcessId, testActionId, "Started" );
        stat1 = getValue( stat1, "//actionStatus" );
        assertEquals("Started", stat1);

        String survXML = getSurvey( "drX", "patient33", testQuestId );
        String confirmQid = getValue( survXML, "//questionName[.='confirm']/../itemId" );

        System.err.println(confirmQid);

        setSurvey("drx", "patient33", testQuestId, confirmQid, "true" );
        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "true", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/confirm" ) );
        assertEquals( "Started", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/status" ) );

        sleep( 500 );

        setSurvey("drx", "patient33", testQuestId, confirmQid, "false" );
        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "false", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/confirm" ) );
        assertEquals( "Started", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/status" ) );

        sleep( 500 );

        setSurvey("drx", "patient33", testQuestId, confirmQid, "true" );
        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "true", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/confirm" ) );
        assertEquals( "Started", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/status" ) );

        sleep( 2500 );


        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "true", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/confirm" ) );
        assertEquals( "Committed", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/status" ) );

        sleep( 2500 );

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "true", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/confirm" ) );
        assertEquals( "Complete", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/status" ) );

        survXML = getSurvey( "drX", "patient33", testQuestId );
        System.err.println( survXML );

        assertEquals( "disable", getValue( survXML, "//action" ) );


        completeDiagnosticGuideProcess( "docX", "patient33", dxProcessId, "Complete"  );

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "Complete", getValue( statusXML, "//gov.hhs.fha.nhinc.kmr2.clinicalAgent.models.decision.DiagnosticGuideProcess/status" ) );

    }







    @Test
    public void testGetRiskModelsDetail() {

        List<String> modelsIds = getElements(getRiskModels("docX", "patient33"), "//modelId");
        String modelStats = getRiskModesDetail( "docX", "patient33", modelsIds.toArray(new String[modelsIds.size()]) );


        String sid1 = getValue( modelStats, "//modelId[.='MockPTSD']/../surveyId" );
        String sid2 = getValue( modelStats, "//modelId[.='MockCold']/../surveyId" );

        assertNotNull( sid1 );
        assertNotNull( sid2 );

        String ptsdSurvey = getSurvey( "docX", "patient33", sid1);
        String coldSurvey = getSurvey( "docX", "patient33", sid2);

        System.out.println("@#*" + coldSurvey);

        String gender = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Gender']/../itemId" );
        String deployments = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Deployments']/../itemId" );
        String alcohol = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Alcohol']/../itemId" );
        String age = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Age']/../itemId" );

        String temperature = getValue( coldSurvey, "//questionName[.='MockCold_Temp']/../itemId" );

        assertNotNull( gender );
        assertNotNull( deployments );
        assertNotNull( age );
        assertNotNull( temperature );

        setSurvey( "drX", "patient33", sid1, deployments, "1" );

        setSurvey( "drX", "patient33", sid1, gender, "female" );
        setSurvey( "drX", "patient33", sid1, alcohol, "yes" );
        setSurvey( "drX", "patient33", sid1, age, "30" );

        setSurvey( "drX", "patient33", sid2, temperature, "39" );

        setRiskThreshold( "drX", "patient33", "MockPTSD", "Alert", 35 );



        modelStats = getRiskModesDetail( "docX", "patient33", modelsIds.toArray(new String[modelsIds.size()]) );


        System.out.println( modelStats );
        assertEquals( "30", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );
        assertEquals( "Average", getValue( modelStats, "//modelId[.='MockPTSD']/../severity" ) );
        assertEquals( "35", getValue( modelStats, "//modelId[.='MockPTSD']/../alertThreshold" ) );
        assertFalse( "Started".equals( getValue( modelStats, "//modelId[.='MockPTSD']/../dxProcessStatus" ) ) );


        assertEquals( "50", getValue( modelStats, "//modelId[.='MockCold']/../alertThreshold" ) );
        assertEquals( "22", getValue( modelStats, "//modelId[.='MockCold']/../relativeRisk" ) );
        assertEquals( "Low", getValue( modelStats, "//modelId[.='MockCold']/../severity" ) );


        setSurvey( "drX", "patient33", sid1, deployments, "null" );
        setSurvey( "drX", "patient33", sid1, gender, "null" );
        setSurvey( "drX", "patient33", sid1, alcohol, "null" );
        setSurvey( "drX", "patient33", sid1, age, "null" );

//        setSurvey( "drX", "patient33", sid2, temperature, "null" );
        System.out.println("@#*" + sid2);
    }





    @Test
    public void testClearRiskModelsSurvey() {


        for ( int j = 0; j < 20; j++ ) {
            System.out.println("\n");
            System.err.println("\n");
        }

        String[] modelsIds = new String[] {"MockCold"};
        String modelStats = getRiskModesDetail( "docX", "patient33", modelsIds  );

        String sid2 = getValue( modelStats, "//modelId[.='MockCold']/../surveyId" );


        assertNotNull( sid2 );

        String coldSurvey = getSurvey( "docX", "patient33", sid2);
        System.out.println("@#*" + coldSurvey);

        String temperature = getValue( coldSurvey, "//questionName[.='MockCold_Temp']/../itemId" );
        assertNotNull( temperature );

        for ( int j = 0; j < 5; j++ ) {
            System.out.println("\n");
            System.err.println("\n");
        }
        System.out.println(  getSurvey( "docX", "patient33", sid2) );
        for ( int j = 0; j < 5; j++ ) {
            System.out.println("\n");
            System.err.println("\n");
        }

        setSurvey( "drX", "patient33", sid2, temperature, "null" );

        setSurvey( "drX", "patient33", sid2, temperature, "39" );


        modelStats = getRiskModesDetail( "docX", "patient33", modelsIds  );



        assertEquals( "22", getValue( modelStats, "//modelId[.='MockCold']/../relativeRisk" ) );


        setSurvey( "drX", "patient33", sid2, temperature, "null" );

        modelStats = getRiskModesDetail( "docX", "patient33", modelsIds  );
        assertEquals( "-1", getValue( modelStats, "//modelId[.='MockCold']/../relativeRisk" ) );




        setSurvey( "drX", "patient33", sid2, temperature, "35" );
        modelStats = getRiskModesDetail( "docX", "patient33", modelsIds  );

        assertEquals( "30", getValue( modelStats, "//modelId[.='MockCold']/../relativeRisk" ) );


    }







    @Test
    // @Ignore
    public void testSetDiagnostic() {

        Map<String,Object> args = new LinkedHashMap<String,Object>();

        String dxProcessReturn = startDiagnosticGuideProcess( "docX", "patient33", "Post Traumatic Stress Disorder");
        String dxProcessId = getValue( dxProcessReturn, "//dxProcessId" );
        System.out.println( dxProcessReturn );

        assertNotNull( dxProcessId );

        String statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );

        String actionId = getValue( statusXML, "//" + testDecModel + ".AskAlcohol/actionId" );
        assertNotNull( actionId );


        System.err.println(statusXML);

        String stat1 = setDiagnosticActionStatus( "drX", "patient33", dxProcessId, actionId, "Started" );
        stat1 = getValue( stat1, "//actionStatus" );
        assertEquals("Started", stat1);

        String stat2 = setDiagnosticActionStatus( "drX", "patient33", dxProcessId, actionId, "Committed" );
        stat2 = getValue( stat2, "//actionStatus" );
        assertEquals("Committed", stat2);

        String stat3 = setDiagnosticActionStatus( "drX", "patient33", dxProcessId, actionId, "Complete" );
        stat3 = getValue( stat3, "//actionStatus" );
        assertEquals("Complete", stat3);

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        System.out.println( statusXML );

        completeDiagnosticGuideProcess( "drX", "patient33", dxProcessId, "Complete" );




    }




    @Test
    // @Ignore
    public void testDifferentialSetSurvey() {


        Map<String,Object> args = new LinkedHashMap<String,Object>();

        String dxProcessReturn = startDiagnosticGuideProcess( "docX", "patient33", "Post Traumatic Stress Disorder");
        String dxProcessId = getValue( dxProcessReturn, "//dxProcessId" );

        assertNotNull( dxProcessId );

        String statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );


        System.err.println(statusXML);


        String testActionId = getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/actionId" );
        String testQuestId = getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/questionnaireId" );
        assertNotNull( testActionId );
        assertNotNull( testQuestId );

        String stat1 = setDiagnosticActionStatus( "drX", "patient33", dxProcessId, testActionId, "Started" );
        stat1 = getValue( stat1, "//actionStatus" );
        assertEquals("Started", stat1);

        String survXML = getSurvey( "drX", "patient33", testQuestId );
        String confirmQid = getValue( survXML, "//questionName[.='confirm']/../itemId" );


        String set;
        set = setSurvey("drx", "patient33", testQuestId, confirmQid, "invalidAnswerForBoolean" );
        assertEquals( "invalid", getValue( set, "//updatedQuestions//itemId[.='"+ confirmQid+"']/../successType" ) );
        System.err.println(set);

        set = setSurvey("drx", "patient33", testQuestId, confirmQid, "true" );

        System.err.println(set);


        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "true", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/confirm" ) );
        assertEquals( "Started", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/status" ) );

        sleep( 2500 );

        set = setSurvey("drx", "patient33", testQuestId, confirmQid, "false" );
        System.out.println( set );
        assertEquals( "disable", getValue( set, "//updatedQuestions//itemId[.='"+ confirmQid+"']/../action" ) );


        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "true", getValue( statusXML, "//" + testDecModel + ".DoExcruciatinglyPainfulTest/confirm" ) );


        completeDiagnosticGuideProcess( "docX", "patient33", dxProcessId, "Complete"  );

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "Complete", getValue( statusXML, "//gov.hhs.fha.nhinc.kmr2.clinicalAgent.models.decision.DiagnosticGuideProcess/status" ) );

    }







    @Test
    // @Ignore
    public void testExceedRiskThreshld() {

        List<String> modelsIds = getElements(getRiskModels("docX", "patient33"), "//modelId");
        String modelStats = getRiskModesDetail( "docX", "patient33", modelsIds.toArray(new String[modelsIds.size()]) );


        String sid1 = getValue( modelStats, "//modelId[.='MockPTSD']/../surveyId" );
        assertNotNull( sid1 );

        String ptsdSurvey = getSurvey( "docX", "patient33", sid1);

        System.out.println(ptsdSurvey);

        String gender = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Gender']/../itemId" );
        String deployments = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Deployments']/../itemId" );
        String alcohol = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Alcohol']/../itemId" );
        String age = getValue( ptsdSurvey, "//questionName[.='MockPTSD_Age']/../itemId" );

        assertNotNull( gender );
        assertNotNull( deployments );
        assertNotNull( age );

        setSurvey( "drX", "patient33", sid1, deployments, "1" );
        setSurvey( "drX", "patient33", sid1, gender, "female" );
        setSurvey( "drX", "patient33", sid1, alcohol, "yes" );
        setSurvey( "drX", "patient33", sid1, age, "30" );

        modelStats = getRiskModesDetail( "docX", "patient33", modelsIds.toArray(new String[modelsIds.size()]) );
        System.out.println( modelStats );
        assertEquals( "30", getValue( modelStats, "//modelId[.='MockPTSD']/../relativeRisk" ) );


        setRiskThreshold( "drX", "patient33", "MockPTSD", "Alert", 05 );






        FactType alertType = mainAgent.getInnerSession("patient33").getKnowledgeBase().getFactType("org.drools.informer.interaction", "Alert");
        Class alertClass = alertType.getFactClass();

        Collection alerts = mainAgent.getInnerSession("patient33").getObjects( new ClassObjectFilter(alertClass) );


        String patientAlertSurveyId = null;
        String ackQuestionId = null;
        for ( Object alert : alerts ) {

            String formId = (String) alertType.get( alert, "formId" );
            String dest = (String) alertType.get( alert, "destination" );

            String form = getSurvey( dest, "patient33", formId );
            assertNotNull( form );

            String type = getValue( form, "//surveyClass" );
            if ("org.kmr2.mock.PatientAcknowledgment".equals( type ) ) {

                patientAlertSurveyId = getValue( form, "//org.drools.informer.presentation.SurveyGUIAdapter/itemId" );

                ackQuestionId = getValue( form, "//org.drools.informer.presentation.QuestionGUIAdapter/questionName[.='ack']/../itemId" );

                assertEquals( formId, patientAlertSurveyId );
                assertNotNull(ackQuestionId);
            }

        }


        setSurvey( "patient33", "patient33", patientAlertSurveyId, ackQuestionId, "accept" );


        sleep(12000);


        alerts = mainAgent.getInnerSession("patient33").getObjects( new ClassObjectFilter(alertClass) );
        assertEquals( 0, alerts.size() );


        setRiskThreshold( "drX", "patient33", "MockPTSD", "Alert", 50 );

    }






    @Test
    public void testDiagnostic() {


        Map<String,Object> args = new LinkedHashMap<String,Object>();

        String dxProcessReturn = startDiagnosticGuideProcess( "docX", "patient33", "Post Traumatic Stress Disorder");
        String dxProcessId = getValue( dxProcessReturn, "//dxProcessId" );


        assertNotNull( dxProcessId );

        String statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );


        System.err.println(statusXML);

        String actionId = getValue( statusXML, "//" + testDecModel + ".AskAlcohol/actionId" );
        String actionQuestId = getValue( statusXML, "//" + testDecModel + ".AskAlcohol/questionnaireId" );
        assertNotNull( actionId );

        String stat1 = setDiagnosticActionStatus( "drX", "patient33", dxProcessId, actionId, "Started" );
        stat1 = getValue( stat1, "//actionStatus" );
        assertEquals("Started", stat1);

        String survXML = getSurvey( "drX", "patient33", actionQuestId );
        String alcoholQid = getValue( survXML, "//questionName[.='question']/../itemId" );

        System.err.println(alcoholQid);

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "false", getValue( statusXML, "//canAdvance") );



        setSurvey( "drX", "patient33", actionQuestId, alcoholQid, "true" );

        survXML = getSurvey( "drX", "patient33", actionQuestId );

//        String stat = setDiagnosticActionStatus( "drX", "patient33", dxProcessId, actionId, "Complete" );
//        assertEquals( "Complete", stat );

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );

        assertEquals( "true", getValue( statusXML, "//actionId[.='"+ actionId + "']/../question" ) );
        assertEquals( "Complete", getValue( statusXML, "//actionId[.='"+ actionId + "']/../status" ) );
        assertEquals( "Not Started", getValue( statusXML, "//" + testDecModel + ".AskDeployment/status" ) );


        assertEquals( "true", getValue( statusXML, "//canAdvance") );
        advanceDiagnosticProcessStatus( "drX", "patient33", dxProcessId );

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        assertEquals( "true", getValue( statusXML, "//canAdvance") );


        completeDiagnosticGuideProcess( "rdrX", "patient33", dxProcessId, "Complete" );

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );

        System.err.println(statusXML);

        assertEquals( "1", getValue( statusXML, "//gov.hhs.fha.nhinc.kmr2.clinicalAgent.models.decision.DxDecision/diseaseProbability[.='10']/../stage" ) );
        assertEquals( "0", getValue( statusXML, "//gov.hhs.fha.nhinc.kmr2.clinicalAgent.models.decision.DxDecision/actions/" + testDecModel + ".AskAlcohol/status[.='Complete']/../../../stage" ) );



    }







    @Test
    // @Ignore
    public void testProbe() {
        System.out.println( probe( "patient33" ) );
    }
























    @Test
    // @Ignore
    public void testEmptyDiagnostic() {


        Map<String,Object> args = new LinkedHashMap<String,Object>();

        String dxProcessReturn = startDiagnosticGuideProcess( "docX", "patient33", "Uncommon Cold");
        String dxProcessId = getValue( dxProcessReturn, "//dxProcessId" );
        assertNotNull( dxProcessId );

        String statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );

        System.err.println( statusXML );

    }



    @Test
    // @Ignore
    public void testNonExistingDiagnostic() {


        Map<String,Object> args = new LinkedHashMap<String,Object>();

        String dxProcessReturn = startDiagnosticGuideProcess( "docX", "patient33", "Imaginary Disease");
        String dxProcessId = getValue( dxProcessReturn, "//dxProcessId" );
        System.err.println( dxProcessId );

        String statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );

        System.err.println( statusXML );

    }













    @Test
    // @Ignore
    public void testAutoCommittActions() {


        Map<String,Object> args = new LinkedHashMap<String,Object>();

        String dxProcessReturn = startDiagnosticGuideProcess( "docX", "patient33", "Post Traumatic Stress Disorder");
        String dxProcessId = getValue( dxProcessReturn, "//dxProcessId" );


        assertNotNull( dxProcessId );

        String statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );




        String actionId = getValue( statusXML, "//" + testDecModel + ".AskSomething1/actionId" );
        String actionQuestId = getValue( statusXML, "//" + testDecModel + ".AskSomething1/questionnaireId" );
        assertNotNull( actionId );
        assertNotNull( actionQuestId );


        String stat1 = setDiagnosticActionStatus( "drX", "patient33", dxProcessId, actionId, "Started" );
        stat1 = getValue( stat1, "//actionStatus" );
        assertEquals("Started", stat1);

        String survXML = getSurvey( "drX", "patient33", actionQuestId );
        String confirmQid = getValue( survXML, "//questionName[.='question']/../itemId" );
        assertNotNull( confirmQid );

        setSurvey( "drX", "patient33", actionQuestId, confirmQid, "true" );

        survXML = getSurvey( "drX", "patient33", actionQuestId );

        statusXML = getDiagnosticProcessStatus( "drX", "patient33", dxProcessId, true );
        System.out.println(statusXML);

        assertEquals( "true", getValue( statusXML, "//actionId[.='"+ actionId + "']/../question" ) );
        assertEquals( "Complete", getValue( statusXML, "//actionId[.='"+ actionId + "']/../status" ) );
        assertEquals( "Not Started", getValue( statusXML, "//" + testDecModel + ".AskDeployment/status" ) );


    }















}








class MockResponseInformer implements DroolsAgentResponseInformer {

    private Map<ACLMessage,List<ACLMessage>> responses = new HashMap<ACLMessage, List<ACLMessage>>();

    public synchronized void informResponse(ACLMessage originalMessage, ACLMessage response) {
        if (!responses.containsKey(originalMessage)){
            responses.put(originalMessage, new ArrayList<ACLMessage>());
        }

        responses.get(originalMessage).add(response);
    }

    public List<ACLMessage> getResponses(ACLMessage originalMessage){
        return this.responses.get(originalMessage);
    }

}