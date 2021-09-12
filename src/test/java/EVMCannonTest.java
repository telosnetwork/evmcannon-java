import org.junit.jupiter.api.Test;

public class EVMCannonTest {

	//@Test
	public void testGarage() throws Exception {
		String rpcUrl = "http://192.168.0.20:7000/evm";
		String privateKey = "0x87ef69a835f8cd0c44ab99b7609a20b2ca7f1c8470af4f0e5b44db927d542084";
		int numBlasters = 1500;
		EVMCannon cannon = new EVMCannon(rpcUrl, privateKey, numBlasters, ".03");
		cannon.initializeAccounts();
		cannon.blast();
	}
}
