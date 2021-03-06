package org.flowable.editor.language.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class CompleteConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
        deployProcess(parsedModel);
    }

    protected String getResource() {
        return "completemodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("userTask1");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("userTask1", flowElement.getId());

        flowElement = model.getMainProcess().getFlowElement("catchsignal");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof IntermediateCatchEvent);
        assertEquals("catchsignal", flowElement.getId());
        IntermediateCatchEvent catchEvent = (IntermediateCatchEvent) flowElement;
        assertEquals(1, catchEvent.getEventDefinitions().size());
        assertTrue(catchEvent.getEventDefinitions().get(0) instanceof SignalEventDefinition);
        SignalEventDefinition signalEvent = (SignalEventDefinition) catchEvent.getEventDefinitions().get(0);
        assertEquals("testSignal", signalEvent.getSignalRef());

        flowElement = model.getMainProcess().getFlowElement("subprocess");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SubProcess);
        assertEquals("subprocess", flowElement.getId());
        SubProcess subProcess = (SubProcess) flowElement;

        flowElement = subProcess.getFlowElement("receiveTask");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof ReceiveTask);
        assertEquals("receiveTask", flowElement.getId());
    }
}
