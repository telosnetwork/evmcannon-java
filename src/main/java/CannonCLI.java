import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class CannonCLI {

    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newFor("evmcannon").build()
                .description("Blast some transactions at an EVM RPC.");
        parser.addArgument("rpc")
                .type(String.class)
                .help("The RPC to send transactions to.  (http://localhost:7000/evm)");

        parser.addArgument("privatekey")
                .type(String.class)
                .help("The private key to begin from");

        parser.addArgument("count")
                .type(Integer.class)
                .help("The number of concurrent transaction cannons to run");

        try {
            Namespace res = parser.parseArgs(args);

            String rpcUrl = res.get("rpc");
            String privateKey = res.get("privatekey");
            int numBlasters = res.get("count");
            EVMCannon cannon = new EVMCannon(rpcUrl, privateKey, numBlasters);
            cannon.initializeAccounts();
            cannon.blast();
        } catch (ArgumentParserException e) {
            parser.handleError(e) c vcvvcb          
        }
    }
}
