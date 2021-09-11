import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import okhttp3.OkHttpClient;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Async;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EVMCannon {

	private String gRpcUrl;
	private Web3j gWeb3;
	private TransactionManager gRootTrxMgr;
	private Credentials gRootCredentials;
	private int gNumBlasters;
	private BigDecimal gTLOSPerBlaster;
	private Map<Credentials, TransactionManager> gAddresses = Maps.newHashMap();
	private Logger gLogger = Logger.getLogger("EVMCannon");
	private Map<String, AtomicInteger> gBlockMap = new ConcurrentHashMap<>();
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static final BigDecimal SPLIT_MULTIPLIER = new BigDecimal(".03");
	private static final int DELAY_MS = 250;
	
	public EVMCannon(String rpcUrl, String privateKey, int numBlasters) throws IOException {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
		gRpcUrl = rpcUrl;
		gWeb3 = Web3j.build(new HttpService(rpcUrl), DELAY_MS, Async.defaultExecutorService());
		gRootCredentials = Credentials.create(privateKey);
		gRootTrxMgr = new FastRawTransactionManager(gWeb3, gRootCredentials, new PollingTransactionReceiptProcessor(gWeb3, DELAY_MS, 40));
		gNumBlasters = numBlasters;
		EthGetBalance balance = gWeb3.ethGetBalance(gRootCredentials.getAddress(), DefaultBlockParameterName.LATEST).send();
		gTLOSPerBlaster = SPLIT_MULTIPLIER.multiply(new BigDecimal(balance.getBalance()).divide(new BigDecimal(gNumBlasters), 2, RoundingMode.HALF_UP));
	}
	
	public void initializeAccounts() throws Exception {
		for (int i = 0; i < gNumBlasters; i++) {
			Web3j web3j = Web3j.build(new HttpService(gRpcUrl, makeHttpClient()), DELAY_MS, Async.defaultExecutorService());
			Credentials cred = Credentials.create(makeKeyPair());
			TransactionManager trxMgr = new FastRawTransactionManager(web3j, cred, new PollingTransactionReceiptProcessor(web3j, DELAY_MS, 40));
			gAddresses.put(cred, trxMgr);
			Transfer transfer = new Transfer(gWeb3, gRootTrxMgr);
			TransactionReceipt receipt = transfer.sendFunds(cred.getAddress(), gTLOSPerBlaster.setScale(0, RoundingMode.FLOOR), Convert.Unit.WEI).send();
			log("Sent to account " + i + " in trx " + receipt.getTransactionHash());
		}
	}
	
	public void blast() {
		List<Thread> threads = Lists.newArrayList();
		gAddresses.forEach((Credentials cred, TransactionManager trxMgr) -> {
			TransferBlasterThread t = new TransferBlasterThread(this, gRpcUrl, DELAY_MS, trxMgr, gRootCredentials.getAddress());
			t.start();
			threads.add(t);
		});
		threads.forEach((Thread t) -> {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}
	
	public void countTransaction(String blockNumber)  {
		if (!gBlockMap.containsKey(blockNumber)) {
			gBlockMap.put(blockNumber, new AtomicInteger(0));
		}
		
		gBlockMap.get(blockNumber).incrementAndGet();
		
		if (gBlockMap.size() % 4 == 0) {
			AtomicInteger total = new AtomicInteger();
			final int[] biggestBlock = {0};
			gBlockMap.forEach((String blockNum, AtomicInteger count) -> {
				total.addAndGet(count.get());
				if (count.get() > biggestBlock[0]) {
					biggestBlock[0] = count.get();
				}
			});
			log("Processed " + total + " transfers, biggest block is " + biggestBlock[0]);
		}
	}
	
	private ECKeyPair makeKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
		return Keys.createEcKeyPair();
	}
	
	private OkHttpClient makeHttpClient() {
		OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(30, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.build();
		return client;
	}
	
	private void log(String message) {
		gLogger.log(Level.INFO, message);
		//System.out.println(DATE_FORMAT.format(new Date()) + " - " + message);
	}
}
