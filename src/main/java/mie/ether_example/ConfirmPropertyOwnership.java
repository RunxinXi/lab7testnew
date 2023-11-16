//package mie.ether_example;
//
//
//import java.util.HashMap;
//import java.util.concurrent.ExecutionException;
//
//import org.flowable.engine.delegate.DelegateExecution;
//import org.flowable.engine.delegate.JavaDelegate;
//import org.web3j.abi.datatypes.Address;
//import org.web3j.abi.datatypes.Utf8String;
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.http.HttpService;
//
//import edu.toronto.dbservice.types.EtherAccount;
//
//
//public class ConfirmPropertyOwnership implements JavaDelegate{
//	
//	@Override
//	public void execute(DelegateExecution execution) {
//		System.out.println("ConfirmPropertyOwnership: IMPLEMENT ME");
//	}
//}
package mie.ether_example;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import edu.toronto.dbservice.types.EtherAccount;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Utf8String;

public class ConfirmPropertyOwnership implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        Web3j web3j = Web3j.build(new HttpService()); // Connect

        String propertyAddress = (String) execution.getVariable("propertyAddress");
        Integer sellerId = (Integer) execution.getVariable("sellerId");

        // Encode property address
        Utf8String encodedPropertyAddress = new Utf8String(propertyAddress);

        // Get property owner's registry address
        Address propertyOwnerAddress = registry.getOwner(encodedPropertyAddress).get();

        // Get seller's registry address
        Address sellerRegistryAddress = accounts.get(sellerId).getCredentials().getAddress();

        // Compare and set the result
        boolean ownershipConfirmed = propertyOwnerAddress.equals(sellerRegistryAddress);
        execution.setVariable("ownershipConfirmed", ownershipConfirmed);

        // Optional: Print addresses for verification
        System.out.println("Property owner's registry address: " + propertyOwnerAddress);
        System.out.println("Seller's registry address: " + sellerRegistryAddress);
    }
}
