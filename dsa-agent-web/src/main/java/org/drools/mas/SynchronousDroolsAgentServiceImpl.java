/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.drools.mas;

import org.apache.cxf.feature.Features;
import org.drools.mas.body.acts.*;
import org.drools.mas.body.content.*;
import org.drools.mas.core.DroolsAgent;
import org.drools.mas.mappers.MyMapArgsEntryType;
import org.drools.mas.mappers.MyMapReferenceEntryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.List;


/**
 * @author salaboy
 * @author esteban
 */
@WebService(targetNamespace = "http://mas.drools.org/")
@XmlSeeAlso(value = {ACLMessage.class, AbstractMessageBody.class, Inform.class, Info.class, QueryIf.class, InformIf.class,
    Agree.class, Failure.class, Action.class, Rule.class, InformRef.class, Act.class,
    QueryRef.class, Query.class, Ref.class, Encodings.class,
    Ref.class, InformRef.class, Request.class, RequestWhen.class,
    MyMapReferenceEntryType.class, MyMapArgsEntryType.class})
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT,
use = SOAPBinding.Use.LITERAL,
parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
@Features(features = "org.apache.cxf.feature.LoggingFeature") 
public class SynchronousDroolsAgentServiceImpl implements SynchronousDroolsAgentService {

    private static Logger logger = LoggerFactory.getLogger(SynchronousDroolsAgentServiceImpl.class);
    private DroolsAgent agent;


    public SynchronousDroolsAgentServiceImpl() {
        
    }

    @WebMethod(exclude = true)
    public void setAgent(DroolsAgent agent) {
        this.agent = agent;
    }

    

    @WebMethod(operationName = "tell")
    public List<ACLMessage> tell(ACLMessage message) {
        if (logger.isDebugEnabled()) {
            logger.debug(" >>> IN Message -> " + message.getPerformative().name());
        }
        try {
            agent.tell(message);
        } catch (Throwable t) {
            if (logger.isErrorEnabled()) {
                logger.error(">>> exception => " + t.getMessage());
                t.printStackTrace();
            }
            return null;

        }
        List<ACLMessage> retrieveResponses = agent.getAgentAnswers(message.getId());
        if (logger.isDebugEnabled()) {
            if (retrieveResponses != null) {
                logger.debug(" <<< Number of OUT Messages -> " + retrieveResponses.size());
                for (ACLMessage outMessage : retrieveResponses) {
                    logger.debug(" <<< OUT Message -> " + outMessage.getPerformative().name());
                }
            } else {
                logger.debug(" <<< 0 OUT Messages");
            }
        }
        return retrieveResponses;
    }
}
