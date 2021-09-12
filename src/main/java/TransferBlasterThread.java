import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.utils.Async;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.logging.Logger;

public class TransferBlasterThread extends Thread {
	
	private EVMCannon gCannon;
	private Web3j gWeb3;
	private TransactionManager gTrxMgr;
	private String gToAddress;
	private Logger gLogger = Logger.getLogger("EVMCannon");
	
	public TransferBlasterThread(EVMCannon cannon, String rpcUrl, int delayMs, TransactionManager trxMgr, String toAddress) {
		gCannon = cannon;
		gWeb3 = Web3j.build(new HttpService(rpcUrl), delayMs, Async.defaultExecutorService());
		gTrxMgr = trxMgr;
		gToAddress = toAddress;
	}
	
	@Override
	public void run() {
		while (true) {
			Transfer transfer = new Transfer(gWeb3, gTrxMgr);
			try {
				if (gCannon.getRunEvery() > 0) {
					long now = Instant.now().getEpochSecond();
					long remainder = now % gCannon.getRunEvery();
					long runAgain = gCannon.getRunEvery() - remainder;
					if (runAgain > 0)
						Thread.sleep(runAgain * 1000);
				}

				TransactionReceipt transactionReceipt = transfer.sendFunds(gToAddress, new BigDecimal("1"), Convert.Unit.WEI).send();
				transactionReceipt.getBlockHash();
				gCannon.countTransaction(transactionReceipt.getBlockNumberRaw());
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
