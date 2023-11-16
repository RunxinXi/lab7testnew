package mie.ether_example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import edu.toronto.dbservice.types.EtherAccount;

import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.FlowableRule;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class Lab7_RealEstateTransaction_UnitTest extends LabBaseUnitTest {
	@BeforeClass
	public static void setupFile() {
		filename = "src/main/resources/diagrams/Lab7_RealEstateTransaction.bpmn";
	}

	String buyerName;
	String sellerName;
	String propertyAddress;
	String soldPrice;
	String fundsInEscrow;
	boolean expectSuccessfulTransaction;

	public Lab7_RealEstateTransaction_UnitTest(String buyerName, String sellerName, String propertyAddress,
			String soldPrice, String fundsInEscrow, String expectSuccessfulTransaction) {
		this.buyerName = buyerName;
		this.sellerName = sellerName;
		this.propertyAddress = propertyAddress;
		this.soldPrice = soldPrice;
		this.fundsInEscrow = fundsInEscrow;
		this.expectSuccessfulTransaction = Boolean.parseBoolean(expectSuccessfulTransaction);
	}

	@Parameters
	public static Collection<String[]> data() {
		ArrayList<String[]> parameters = new ArrayList<>();
		parameters.add(new String[] { "Aimee Lowe", "Lauryn Talbot", "5 King's College Rd, Toronto, ON M5S 3G8",
				"300000", "true", "true" });
		return parameters;
	}

	@Test
	public void testRealEstateTransaction() {
		RuntimeService runtimeService = flowableContext.getRuntimeService();
		processInstance = runtimeService.startProcessInstanceByKey("process1");
		fillTransactionForm();
		
		boolean fundsReleased = (boolean) flowableContext.getHistoryService().createHistoricVariableInstanceQuery().variableName("fundsReleased").singleResult().getValue();
		
		if (expectSuccessfulTransaction) {
			assertTrue(fundsReleased);
			assertTrue(checkOwnership());
		} else {
			assertFalse(fundsReleased);
		}
	}

	private void fillTransactionForm() {
		TaskService taskService = flowableContext.getTaskService();
		Task transactionForm = taskService.createTaskQuery().taskDefinitionKey("transactionForm")
				.singleResult();
		
		HashMap<String, String> formEntries = new HashMap<>();
		formEntries.put("buyerName", buyerName);
		formEntries.put("sellerName", sellerName);
		formEntries.put("propertyAddress", propertyAddress);
		formEntries.put("soldPrice", soldPrice);
		formEntries.put("fundsInEscrow", fundsInEscrow);
		
		flowableContext.getFormService().submitTaskFormData(transactionForm.getId(), formEntries);
	}
	
	private boolean checkOwnership() {
		HashMap<Integer, EtherAccount> accounts = (HashMap<Integer, EtherAccount>) flowableContext.getHistoryService().createHistoricVariableInstanceQuery().variableName("accounts").singleResult().getValue();
		int buyerId = (int) flowableContext.getHistoryService().createHistoricVariableInstanceQuery().variableName("buyerId").singleResult().getValue();
		String buyerAddress = accounts.get(buyerId).getCredentials().getAddress();
		
		Web3j web3 = Web3j.build(new HttpService());
		String contractAddress = (String)  flowableContext.getHistoryService().createHistoricVariableInstanceQuery().variableName("contractAddress").singleResult().getValue();
		Registry registry = Registry.load(contractAddress, web3, accounts.get(0).getCredentials(), EtherUtils.GAS_PRICE, EtherUtils.GAS_LIMIT_CONTRACT_TX);
		
		Utf8String encodedPropertyAddress = new Utf8String(propertyAddress);
		
		try {
			return registry.getOwner(encodedPropertyAddress).get().toString().equals(buyerAddress);
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	//Q6
	@Test
	public void testTransactionBuyerClientOnly() {
	    // Set up the process variables
	    HashMap<String, Object> variables = new HashMap<>();
	    variables.put("buyerName", "BuyerClient");
	    variables.put("sellerName", "NonClientSeller");
	    variables.put("propertyAddress", "123 Main St");
	    variables.put("soldPrice", "500000");
	    variables.put("fundsInEscrow", "true");

	    // Start the process with these variables
	    RuntimeService runtimeService = flowableContext.getRuntimeService();
	    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process1", variables);

	    // Use HistoryService to query the process variable
	    HistoryService historyService = flowableContext.getHistoryService();
	    String transactionStatus = (String) historyService.createHistoricVariableInstanceQuery()
	                                    .processInstanceId(processInstance.getId())
	                                    .variableName("transactionStatus")
	                                    .singleResult()
	                                    .getValue();
	    assertEquals("Proceed", transactionStatus);
	}
	//Q7
	@Test
	public void testTransactionBothClient() {
	    // Set up the process variables
	    HashMap<String, Object> variables = new HashMap<>();
	    variables.put("buyerName", "BuyerClient");
	    variables.put("sellerName", "SellerClient");
	    variables.put("propertyAddress", "456 Park Ave");
	    variables.put("soldPrice", "600000");
	    variables.put("fundsInEscrow", "true");

	    // Start the process with these variables
	    RuntimeService runtimeService = flowableContext.getRuntimeService();
	    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process1", variables);

	 // Use HistoryService to query the process variable
	    HistoryService historyService = flowableContext.getHistoryService();
	    String transactionStatus = (String) historyService.createHistoricVariableInstanceQuery()
	                                    .processInstanceId(processInstance.getId())
	                                    .variableName("transactionStatus")
	                                    .singleResult()
	                                    .getValue();
	    assertEquals("Proceed", transactionStatus);
	}
	//Q8
	@Test
	public void testTransactionBuyerClientSellerNotOwner() {
	    // Set up the process variables
	    HashMap<String, Object> variables = new HashMap<>();
	    variables.put("buyerName", "BuyerClient");
	    variables.put("sellerName", "FakeOwner");
	    variables.put("propertyAddress", "789 Elm St");
	    variables.put("soldPrice", "400000");
	    variables.put("fundsInEscrow", "true");

	    // Start the process with these variables
	    RuntimeService runtimeService = flowableContext.getRuntimeService();
	    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process1", variables);

	    // Add assertions to check the expected outcome
	 // Use HistoryService to query the process variable
	    HistoryService historyService = flowableContext.getHistoryService();
	    String transactionStatus = (String) historyService.createHistoricVariableInstanceQuery()
	                                    .processInstanceId(processInstance.getId())
	                                    .variableName("transactionStatus")
	                                    .singleResult()
	                                    .getValue();
	    assertEquals("Cancel", transactionStatus);
	}




}
