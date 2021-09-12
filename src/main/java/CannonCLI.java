import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.time.Instant;


public class CannonCLI {

    private static final String DEFAULT_RPC = "http://localhost:7000/evm";
    private static final int DEFAULT_BLASTERS = 150;

    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newFor("evmcannon").build()
                .description("Blast some transactions at an EVM RPC.");
        parser.addArgument("-r", "--rpc")
                .type(String.class)
                .setDefault(DEFAULT_RPC)
                .help("The RPC to send transactions to.  (default: " + DEFAULT_RPC + " )");

        parser.addArgument("-p", "--private-key")
                .type(String.class)
                .help("The private key to begin from");

        parser.addArgument("-c", "--count")
                .type(Integer.class)
                .setDefault(DEFAULT_BLASTERS)
                .help("The number of concurrent transaction cannons to run. (default: " + DEFAULT_BLASTERS + " )");

        parser.addArgument("-i", "--interval")
                .type(Integer.class)
                .setDefault(0)
                .help("The interval on which to send transactions, creates a pulse/surge effect. (default: 0)");

        parser.addArgument("-s", "--start-at")
                .type(Long.class)
                .setDefault(0L)
                .help("The time as epoch in seconds to start blasting.  (default: 0, start right away)");

        parser.addArgument("-pct", "--percent")
                .type(Long.class)
                .setDefault(3L)
                .help("The percent of the balance from the private-key to use for the blasters. (must be 0-100, default: 3)");

        try {
            Namespace res = parser.parseArgs(args);
            String rpcUrl = res.get("rpc");
            String privateKey = res.get("private_key");
            int numBlasters = res.get("count");
            int interval = res.get("interval");
            long startAt = res.get("start_at");
            long percentToUse = res.get("percent");
            if (percentToUse > 100 || percentToUse <= 0)
                throw new Exception("Percent must be 0-100");

            String percentToUseString = Double.valueOf(percentToUse / 100D).toString();
            EVMCannon cannon = new EVMCannon(rpcUrl, privateKey, numBlasters, percentToUseString);
            cannon.setRunEvery(interval);
            cannon.initializeAccounts();
            long now = Instant.now().getEpochSecond();
            if (startAt > now)
                Thread.sleep((startAt - now) * 1000);

            cannon.blast();
        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }
    }
}
