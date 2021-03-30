package me.cyz.bulk;

import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.abi.datatypes.Int;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.signature.ECDSASignature;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.transaction.builder.TransactionBuilderService;
import org.fisco.bcos.sdk.transaction.codec.encode.TransactionEncoderService;
import org.fisco.bcos.sdk.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.transaction.model.po.RawTransaction;
import org.fisco.bcos.sdk.transaction.pusher.TransactionPusherService;
import org.fisco.bcos.sdk.transaction.signer.TransactionSignerServcie;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author aaronchu
 * @Description
 * @data 2021/03/30
 */
public class App {

    public static final String ABI = IOUtil.readResourceAsString("abi/HelloWorld.abi");

    public static final String BINARY = IOUtil.readResourceAsString("bin/ecc/HelloWorld.bin");

    public static final String SM_BINARY = IOUtil.readResourceAsString("bin/sm/HelloWorld.bin");


    public static void main(String[] args) throws Exception{
        final int txCount = Integer.parseInt(args[0]);
        final int threads = Integer.parseInt(args[1]);

        BcosSDK sdk =  BcosSDK.build("conf/config.toml");
        Client client = sdk.getClient(1);
        AssembleTransactionProcessor txProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(client,client.getCryptoSuite().createKeyPair());
        String addr = deploy(txProcessor, ABI, BINARY);
        long start = System.currentTimeMillis();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        CountDownLatch latch = new CountDownLatch(txCount);
        for(int i=0;i<txCount;i++){
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try{
                        txProcessor.sendTransactionAndGetResponse(addr, ABI, "set", Arrays.asList("caliper"));
                    }
                    catch (Exception ex){}
                    finally {
                        latch.countDown();
                    }
                }
            });

        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("Finished, time cost:"+(end - start) + " milliseconds");
        System.exit(0);
    }

    private static String deploy(AssembleTransactionProcessor txProcessor, String abi, String bin) throws Exception{
        TransactionResponse tr = txProcessor.deployAndGetResponse(abi, bin, Arrays.asList());
        return tr.getContractAddress();
    }

}
