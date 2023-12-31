package mie.ether_example;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import edu.toronto.dbservice.config.MIE354DBHelper;
import edu.toronto.dbservice.types.EtherAccount;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class DeployAndInitializeRealEstateRegistry implements JavaDelegate {
	public static final BigInteger FUND_AMOUNT = BigInteger.valueOf(25000000000000000L);

	@SuppressWarnings("unchecked")
	@Override
	public void execute(DelegateExecution execution) {

		// Connect to the blockchain server
		Web3j web3 = Web3j.build(new HttpService());

		// Get list of accounts and provide initial funding to each account
		HashMap<Integer, EtherAccount> accounts = (HashMap<Integer, EtherAccount>) execution.getVariable("accounts");
		try {
			fundAccounts(web3, accounts, FUND_AMOUNT);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// deploy a new registry contract
		Registry myRegistry = null;
		try {
			myRegistry = Registry.deploy(web3, accounts.get(0).getCredentials(), EtherUtils.GAS_PRICE,
					EtherUtils.GAS_LIMIT_CONTRACT_TX, BigInteger.ZERO).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TransactionReceipt deployReceipt = myRegistry.getTransactionReceipt().get();
		EtherUtils.reportTransaction("Contract deployed at " + myRegistry.getContractAddress(), deployReceipt);

		registerPropertyOwnerships(web3, accounts, myRegistry.getContractAddress());
		
		// save contract address as a process variable
		execution.setVariable("contractAddress", myRegistry.getContractAddress());
	}

	private void fundAccounts(Web3j web3, HashMap<Integer, EtherAccount> accounts, BigInteger amountPerAccount)
			throws Exception {
		EthCoinbase coinbase = web3.ethCoinbase().sendAsync().get();

		for (EtherAccount account : accounts.values()) {
			Credentials credentials = account.getCredentials();
			EthGetTransactionCount transactionCount = web3
					.ethGetTransactionCount(coinbase.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();

			BigInteger nonce = transactionCount.getTransactionCount();

			Transaction aliceMoney = Transaction.createEtherTransaction(coinbase.getAddress(), nonce,
					EtherUtils.GAS_PRICE, EtherUtils.GAS_LIMIT_ETHER_TX.multiply(BigInteger.valueOf(2)),
					credentials.getAddress(), BigInteger.valueOf(25000000000000000L));

			EthSendTransaction sendTransaction = web3.ethSendTransaction(aliceMoney).sendAsync().get();
			TransactionReceipt transactionReceipt = web3.ethGetTransactionReceipt(sendTransaction.getTransactionHash())
					.sendAsync().get().getResult();
			EtherUtils.reportTransaction("Funded account " + credentials.getAddress(), transactionReceipt);
		}
	}
	
	private void registerPropertyOwnerships(Web3j web3,  HashMap<Integer, EtherAccount> accounts, String contractAddress) {
		System.out.println("hello");
		Connection conn = MIE354DBHelper.getDBConnection();
		String sql = "SELECT * FROM PROPERTIES;";
		ResultSet rs = null;
		try {
			rs = conn.createStatement().executeQuery(sql);
			while(rs.next()) {
				int originalOwner = rs.getInt("ORIGINALOWNER");
				String address = rs.getString("ADDRESS");
				if (originalOwner != -1) {					
					Utf8String encodedAddress = new Utf8String(address);
					Registry reg = Registry.load(contractAddress, web3, accounts.get(originalOwner).getCredentials(), EtherUtils.GAS_PRICE, EtherUtils.GAS_LIMIT_CONTRACT_TX);
					reg.register(encodedAddress);
					System.out.printf("Registered\n%s\nto Account %d\n", address, originalOwner);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
